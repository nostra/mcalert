package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.AlertModel;
import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class AlertResource {
    private static final Logger logger = LoggerFactory.getLogger(AlertResource.class);
    private final AlertEndpointConfig alertEndpointConfig;

    Map<String, AlertCaller> alertEndpointMap;

    @ConfigProperty(name = "mcalert.ignore.alerts")
    List<String> namesToIgnore;

    /**
     * Watchdog alerts are alerts that should fire, they don't there is an error.
     */
    @ConfigProperty(name = "mcalert.watchdog.alerts")
    List<String> watchdogAlertNames;

    public AlertResource(AlertEndpointConfig alertEndpointConfig) {
        this.alertEndpointConfig = alertEndpointConfig;
    }

    @PostConstruct
    void init() {
        alertEndpointMap = createClientMap();
        clearListIfDisabled(namesToIgnore);
        clearListIfDisabled(watchdogAlertNames);
    }

    private Map<String, AlertCaller> createClientMap() {
        return alertEndpointConfig.endpoints()
                .entrySet()
                .stream()
                .map(entry ->
                        Map.entry(entry.getKey(),
                                RestClientBuilder.newBuilder()
                                        .baseUri(entry.getValue().uri())
                                        .connectTimeout(2, TimeUnit.SECONDS)
                                        .readTimeout(5, TimeUnit.SECONDS)
                                        .build(AlertCaller.class)
                        ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * If there is a communication problem, an exception is thrown
     * @return data structure with only firing and relevant alerts. Ensure presence
     * of watchdog alert
     */
    public Map<String, PrometheusResult> getFiringAndRelevant() {
        return alertEndpointMap.entrySet()
                .stream()
                .map(this::processAlertService)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Process one and one endpoint
     * @return An entry where the key is the configuration name
     */
    private Map.Entry<String, PrometheusResult> processAlertService(Map.Entry<String, AlertCaller> entry) {
        PrometheusResult result;
        try {
            result = entry.getValue().callPrometheus().addName(entry.getKey());
            List<AlertModel> toRemove = extractIrrelevantAlerts(result);
            result.data().alerts().removeAll(toRemove);
            ensureWatchdogPresence(result, toRemove);
        } catch (Exception e) {
            // Log and rethrow
            logger.error("Got exception for [" + entry.getKey() + "] - masked: " + e);
            throw e;
        }
        return Map.entry(result.name(), result);
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
