package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.config.AlertEndpointConfig;
import io.github.nostra.mcalert.model.AlertModel;
import io.github.nostra.mcalert.model.FiringAlertMeta;
import io.github.nostra.mcalert.model.GrafanaDatasource;
import io.github.nostra.mcalert.model.PrometheusData;
import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SingleEndpointPoller {
    private static final Logger log = LoggerFactory.getLogger(SingleEndpointPoller.class);

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final AlertCaller caller;
    private final List<String> namesToIgnore;
    /**
     * Watchdog alerts are alerts that should fire, if they don't there is an error.
     */
    private final List<String> watchdogAlertNames;
    private int numAlerts = -42;
    private boolean active = true;
    private Map<String, FiringAlertMeta> firing = new HashMap<>();


    public SingleEndpointPoller(AlertEndpointConfig.AlertEndpoint config, AlertCaller caller) {
        this.caller = caller;
        this.watchdogAlertNames = config.watchdogAlerts();
        this.namesToIgnore = config.ignoreAlerts();
        clearListIfDisabled(namesToIgnore);
        clearListIfDisabled(watchdogAlertNames);
    }

    public SingleEndpointPoller(AlertEndpointConfig.AlertEndpoint config) {
        this(config, createRestClient(config).build(AlertCaller.class));
    }

    private static RestClientBuilder createRestClient(AlertEndpointConfig.AlertEndpoint config) {
        return createRestClient(config.uri(), config.header().orElse(List.of()));
    }

    private static RestClientBuilder createRestClient(URI uri, List<AlertEndpointConfig.Header> headers) {
        RestClientBuilder builder = RestClientBuilder.newBuilder().baseUri(uri).followRedirects(true).connectTimeout(2, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS);
        headers.forEach(header -> builder.register((ClientRequestFilter) requestContext -> requestContext.getHeaders().add(header.name(), header.content())));

        return builder;
    }

    /**
     * @return Return value is filtered for watchdog and irrelevant alerts
     */
    public PrometheusResult callPrometheus(String name) {
        if (!active) {
            return new PrometheusResult("success", new PrometheusData(Collections.emptyList()), name);
        }

        try {
            PrometheusResult result = caller.callPrometheus().addName(name);
            updateFiringMapWith( result.data().alerts() );
            List<AlertModel> toRemove = extractIrrelevantAlerts(result);
            result.data().alerts().removeAll(toRemove);
            ensureWatchdogPresence(result, toRemove);

            int current = result.noAlerts() ? 0 : result.data().alerts().size();

            log.trace("Calling api endpoint for configuration {}. Got {} alerts", name, current);

            pcs.firePropertyChange("numAlerts", numAlerts, current);
            if ( numAlerts != current && current >0 ) {
                // Just log changed alerts for the moment. Note that it would not fire if one alert were exchanged with another...
                log.debug("New alert(s) triggered: {}",
                        result.data()
                                .alerts()
                                .stream()
                                .flatMap(a -> a.labels().entrySet().stream())
                                .filter(map -> map.getKey().equals("alertname"))
                                .map(Map.Entry::getValue)
                                .collect(Collectors.toSet()));
            }
            numAlerts = current;
            return result;
        } catch (Exception e) {
            pcs.firePropertyChange("numAlerts", numAlerts, -2);
            numAlerts = -2;
            throw e;
        }
    }

    private void updateFiringMapWith(List<AlertModel> result) {
        result.stream()
                .filter(am -> "firing".equals(am.state()))
                .forEach(am -> {
                    FiringAlertMeta fam = firing.get(am.alertName());
                    if ( fam == null ) {
                        fam = new FiringAlertMeta(
                                am.alertName(),
                                1,
                                Instant.now()
                        );
                    } else {
                        fam = fam.increment();
                    }
                    firing.put(Objects.requireNonNull(fam).name(), fam);
                });
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    public void toggleActive() {
        active = !active;
        pcs.firePropertyChange("numAlerts", numAlerts, -666);
        numAlerts = -666;
    }

    private void ensureWatchdogPresence(PrometheusResult result, List<AlertModel> toRemove) {
        if (result.data().alerts().isEmpty()) {
            // Ensure that watchdog alerts are present if nothing else fires
            result.data().alerts().addAll(findMissingWatchDogAlerts(toRemove));
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
        Predicate<AlertModel> irrelevantModels =
                alertModel -> !"firing".equals(alertModel.state())
                || namesToIgnore.contains(alertModel.alertName())
                || watchdogAlertNames.contains(alertModel.alertName());

        return result.data().alerts().stream().filter(irrelevantModels).toList();
    }

    private List<AlertModel> findMissingWatchDogAlerts(List<AlertModel> toRemove) {
        Map<String, AlertModel> watchDogAlerts = new HashMap<>();
        toRemove.stream().filter(alertModel -> "firing".equals(alertModel.state())).filter(alertModel -> watchdogAlertNames.contains(alertModel.alertName())).forEach(alertModel -> watchDogAlerts.putIfAbsent(alertModel.alertName(), alertModel));
        if (watchDogAlerts.size() != watchdogAlertNames.size()) {
            return watchdogAlertNames.stream().filter(name -> !watchDogAlerts.containsKey(name)).map(name -> new AlertModel(Collections.emptyMap(), Map.of("alertName", name), "missing", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT), 1L)).toList();
        }
        return Collections.emptyList();
    }

    public boolean isActive() {
        return active;
    }

    /// @return A list of datasources provided by grafana
    public List<GrafanaDatasource> callGrafanaForDs() {
        return caller.callGrafana();
    }

    /// Interim method to see which alarts are triggering and not. This
    /// will later be presented as a popup
    public void outputStatus() {
        log.info("Ignoring: {}", namesToIgnore);
        log.info("Also ignoring watchdog alerts: {} ", namesToIgnore);
        firing.values().forEach(fire -> log.info("==> {}",fire));
    }
}
