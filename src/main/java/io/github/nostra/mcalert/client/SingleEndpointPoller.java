package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.MaclertTab;
import io.github.nostra.mcalert.config.AlertEndpointConfig;
import io.github.nostra.mcalert.model.AlertModel;
import io.github.nostra.mcalert.model.AlertType;
import io.github.nostra.mcalert.model.FiringAlertMeta;
import io.github.nostra.mcalert.model.GrafanaDatasource;
import io.github.nostra.mcalert.model.PrometheusData;
import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SingleEndpointPoller {
    private static final Logger log = LoggerFactory.getLogger(SingleEndpointPoller.class);

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final AlertCaller caller;
    private final List<String> namesToIgnore;
    /**
     * Watchdog alerts are alerts that should fire, if they don't there is an error.
     */
    private final List<String> watchdogAlertNames;
    private boolean active = true;
    private Map<String, FiringAlertMeta> firing = new HashMap<>();
    private FiringAlertMeta[] firingAlerts = new FiringAlertMeta[0];
    private String resourceKey;
    /// Tab is the tab pane the alert belongs to
    private MaclertTab tab;

    public SingleEndpointPoller(AlertEndpointConfig.AlertEndpoint config, AlertCaller caller) {
        this.caller = caller;
        this.watchdogAlertNames = config.watchdogAlerts();
        this.namesToIgnore = config.ignoreAlerts();
        clearListIfDisabled(namesToIgnore);
        clearListIfDisabled(watchdogAlertNames);

        // Iterate over the union set and add each item to the firing hashmap in order to see which that never fires
        Stream.concat(watchdogAlertNames.stream(), namesToIgnore.stream())
                .forEach(alertName -> firing
                        .putIfAbsent(alertName, new FiringAlertMeta(resourceKey,alertName, 0, null, AlertType.INACTIVE)));
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
    public PrometheusResult callPrometheus(String resourceKeyAsParam) {
        if (!active) {
            firingAlerts = new FiringAlertMeta[]{new FiringAlertMeta(resourceKey, resourceKeyAsParam, 0, Instant.now(), AlertType.DEACTIVATED)};
            pcs.firePropertyChange("firingAlerts", null, firingAlerts);
            return new PrometheusResult("success", new PrometheusData(Collections.emptyList()), resourceKeyAsParam);
        }

        try {
            PrometheusResult result = caller.callPrometheus().addName(resourceKeyAsParam);
            updateFiringMapWith(result.data().alerts());
            List<AlertModel> toRemove = extractIrrelevantAlerts(result);
            result.data().alerts().removeAll(toRemove);
            ensureWatchdogPresence(result, toRemove);

            List<FiringAlertMeta> currentAlerts = result.data().alerts().stream()
                    .map(alert -> new FiringAlertMeta(
                            resourceKey,
                            (alert.alertName()==null ? "ERROR" : alert.alertName()),
                            1,
                            Instant.now(),
                            AlertType.ACTIVE))
                    .collect(Collectors.toList());

            if (currentAlerts.isEmpty()) {
                currentAlerts.add(new FiringAlertMeta(resourceKey,resourceKeyAsParam, 0, Instant.now(), AlertType.INACTIVE));
            }

            FiringAlertMeta[] newFiringAlerts = currentAlerts.toArray(new FiringAlertMeta[0]);
            log.trace("Calling api endpoint for configuration {}. Got {} alerts", resourceKeyAsParam, currentAlerts.size());

            pcs.firePropertyChange("firingAlerts", firingAlerts, newFiringAlerts);
            boolean toUpdate = false;
            if (anyChangesComparedToFiringAlerts( newFiringAlerts )) {
                log.debug("New alert(s) triggered: {}",
                        currentAlerts.stream()
                                .map(fam -> fam.resourceKey() + "." + fam.name()+"."+fam.alertType().name())
                                .peek(aname -> {
                                    if ( aname == null ) {
                                        log.error("MISSING NAME. Current alerts: "+currentAlerts);
                                    }
                                })
                                .collect(Collectors.toSet()));
                toUpdate = true;
            }
            firingAlerts = newFiringAlerts;
            if ( toUpdate && tab != null ) {
                log.trace("Shall update tab {}", tab.getText());
                tab.updateContentsOfTab(this);
            }
            return result;
        } catch (Exception e) {
            FiringAlertMeta[] errorAlerts = {new FiringAlertMeta(resourceKey, resourceKeyAsParam, -2, Instant.now(), AlertType.INACTIVE)};
            pcs.firePropertyChange("firingAlerts", firingAlerts, errorAlerts);
            firingAlerts = errorAlerts;
            throw e;
        }
    }

    private boolean anyChangesComparedToFiringAlerts(FiringAlertMeta[] newFiringAlerts) {
        if ( firingAlerts.length != newFiringAlerts.length ) {
            return true;
        }
        for ( int i=0 ; i < firingAlerts.length ; i++ ) {
            if ( firingAlerts[i] == null || firingAlerts[i].name() == null ) {
                log.warn("Object or name is null? Unexpected, this is item {} out of {}", i, firingAlerts.length - 1 );
                return true;
            }
            else if (! firingAlerts[i].name().equals(newFiringAlerts[i].name())) {
                log.info("{} != {}", firingAlerts[i].name(), newFiringAlerts[i].name());
                return true;
            }
        }
        return false;
    }

    private void updateFiringMapWith(List<AlertModel> result) {
        result.stream()
                .filter(am -> "firing".equals(am.state()))
                .forEach(am -> {
                    FiringAlertMeta fam = firing.get(am.alertName());
                    if ( fam == null ) {
                        fam = new FiringAlertMeta(
                                resourceKey,
                                am.alertName(),
                                1,
                                Instant.now(),
                                AlertType.ACTIVE
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

    public void firePropertyChange( String name, Object oldValue, Object newValue ) {
        PropertyChangeEvent event = new PropertyChangeEvent(this, name, oldValue, newValue);
        log.warn("Firing {} with value {}", name, newValue);
        pcs.firePropertyChange(event);
    }

    public void toggleActive() {
        active = !active;
        FiringAlertMeta[] deactivatedAlerts = firing.values().stream()
                .map(alert -> new FiringAlertMeta(
                        resourceKey,
                        alert.name(),
                        alert.numberOfAlerts(),
                        Instant.now(),
                        AlertType.DEACTIVATED))
                .toArray(FiringAlertMeta[]::new);
        pcs.firePropertyChange("firingAlerts", firingAlerts, deactivatedAlerts);
        firingAlerts = deactivatedAlerts;
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
            return watchdogAlertNames.stream()
                    .filter(name -> !watchDogAlerts.containsKey(name))
                    .map(name -> new AlertModel(Collections.emptyMap(),
                            Map.of("alertname", name), "missing",
                            ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT), "1"))
                    .toList();
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
        log.info("Also ignoring watchdog alerts: {} ", watchdogAlertNames);
        firing.values().forEach(fire -> log.info("==> {}",fire));
    }

    ///  Union of alerts to ignore and watchdog alerts
    public List<String> ignoredAlerts() {
        return Stream.concat(namesToIgnore.stream(), watchdogAlertNames.stream())
                .sorted()
                .toList();
    }

    public List<FiringAlertMeta> firingAlerts() {
        return new ArrayList<>(firing.values());
    }

    ///  @return true if alert with given name is a watchdog alert
    public boolean isWatchDogAlert(String name) {
        return watchdogAlertNames.contains(name);
    }

    public boolean toggleIgnoreOn(String name) {
        if ( isWatchDogAlert(name)) {
            // Disallowing to toggle of watchdog alert
            return false;
        }
        if ( namesToIgnore.contains(name)) {
            namesToIgnore.remove(name);
        } else {
            namesToIgnore.add(name);
        }
        return true;
    }

    ///  Set the configuration key used for this poller
    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public void setTab(MaclertTab tab) {
        if ( this.tab != null ) {
            log.error("Inconsistency in set tab, it is called more than once.");
        }
        this.tab = tab;
    }
}
