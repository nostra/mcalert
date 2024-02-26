package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.AlertModel;
import io.github.nostra.mcalert.model.PrometheusResult;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class SingleEndpointPoller {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private AlertCaller caller;
    private int numAlerts = -42;

    List<String> namesToIgnore;

    /**
     * Watchdog alerts are alerts that should fire, if they don't there is an error.
     */
    List<String> watchdogAlertNames;


    public SingleEndpointPoller( AlertEndpointConfig.AlertEndpoint config, AlertCaller caller ) {
        this.caller = caller;
        this.watchdogAlertNames = config.watchdogAlerts();
        this.namesToIgnore = config.ignoreAlerts();
        clearListIfDisabled(namesToIgnore);
        clearListIfDisabled(watchdogAlertNames);
    }

    public SingleEndpointPoller(AlertEndpointConfig.AlertEndpoint config) {
        this( config, RestClientBuilder.newBuilder()
                .baseUri(config.uri())
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build(AlertCaller.class));
    }

    /**
     * @return Return value is filtered for watchdog and irrelevant alerts
     */
    public PrometheusResult callPrometheus(String name) {
        try {
            PrometheusResult result = caller.callPrometheus().addName(name);
            List<AlertModel> toRemove = extractIrrelevantAlerts(result);
            result.data().alerts().removeAll(toRemove);
            ensureWatchdogPresence(result, toRemove);

            int current = result.noAlerts() ? 0 : result.data().alerts().size();
            pcs.firePropertyChange("numAlerts", numAlerts, current);
            numAlerts = current;
            return result;
        } catch (Exception e ) {
            pcs.firePropertyChange("numAlerts", numAlerts, -2);
            numAlerts = -2;
            throw e;
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    private void ensureWatchdogPresence(PrometheusResult result, List<AlertModel> toRemove) {
        if (result.data().alerts().isEmpty()) {
            // Ensure that watchdog alerts are present if nothing else fires
            result.data()
                    .alerts()
                    .addAll(findMissingWatchDogAlerts(toRemove));
        }
    }

    /**
     * Quarkus does not let us easily configure empty list. Having a entry which
     * flags emptiness fixes this
     */
    private void clearListIfDisabled(List<String> list) {
        if (List.of("disabled").containsAll(list)) {
            list.clear();
        }
    }

    /**
     * Irrelevant alerts are those that are not "firing", or that are in the ignore
     * or watchdog list
     */
    private List<AlertModel> extractIrrelevantAlerts(PrometheusResult result) {
        Predicate<AlertModel> irrelevantModels = alertModel ->
                !"firing".equals(alertModel.state())
                        || namesToIgnore.contains(alertModel.alertName())
                        || watchdogAlertNames.contains(alertModel.alertName());

        return result.data().alerts().stream()
                .filter(irrelevantModels)
                .toList();
    }

    private List<AlertModel> findMissingWatchDogAlerts(List<AlertModel> toRemove) {
        Map<String, AlertModel> watchDogAlerts = new HashMap<>();
        toRemove
                .stream()
                .filter(alertModel -> "firing".equals(alertModel.state()))
                .filter(alertModel -> watchdogAlertNames.contains(alertModel.alertName()))
                .forEach(alertModel ->
                        watchDogAlerts.putIfAbsent(alertModel.alertName(), alertModel)
                );
        if (watchDogAlerts.size() != watchdogAlertNames.size()) {
            return watchdogAlertNames.stream()
                    .filter(name -> !watchDogAlerts.containsKey(name))
                    .map(name -> new AlertModel(Collections.emptyMap(),
                            Map.of("alertName", name),
                            "missing",
                            ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
                            1L)
                    )
                    .toList();
        }
        return Collections.emptyList();
    }
}
