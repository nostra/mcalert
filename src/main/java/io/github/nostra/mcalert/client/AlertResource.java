package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.AlertModel;
import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    Map<String, AlertService> alertService;

    @ConfigProperty(name = "mcalert.ignore.alerts")
    List<String> namesToIgnore;

    /**
     * Watchdog alerts are alerts that should fire, they don't there is an error.
     */
    @ConfigProperty(name = "mcalert.watchdog.alerts")
    List<String> watchdogAlertNames;

    @Inject
    AlertEndpointConfig alertEndpointConfig;

    @PostConstruct
    void init() {
        alertService = createClientMap();
        clearListIfDisabled(namesToIgnore);
        clearListIfDisabled(watchdogAlertNames);
    }

    public Map<String, AlertService> createClientMap() {
        return alertEndpointConfig.endpoints()
                .entrySet()
                .stream()
                .map(entry ->
                        Map.entry(entry.getKey(),
                                RestClientBuilder.newBuilder()
                                        .baseUri(entry.getValue().uri())
                                        .connectTimeout(2, TimeUnit.SECONDS)
                                        .readTimeout(5, TimeUnit.SECONDS)
                                        .build(AlertService.class)
                        ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * @return data structure with only firing and relevant alerts. Ensure presence
     * of watchdog alert
     */
    public Map<String, PrometheusResult> getFiringAndRelevant() {
        return alertService.entrySet()
                .stream()
                //.map(entry -> Map.entry( entry.getKey(), entry.getValue().callPrometheus()))
                .map(entry -> {
                    try {
                        var result = entry.getValue().callPrometheus();
                        List<AlertModel> toRemove = extractIrrelevantAlerts(result);
                        result.data().alerts().removeAll(toRemove);
                        if (result.data().alerts().isEmpty()) {
                            // Ensure that watchdog alerts are present if nothing else fires
                            result.data()
                                    .alerts()
                                    .addAll(findMissingWatchDogAlerts(toRemove));
                        }
                        return Map.entry(entry.getKey(), result);
                    } catch (Exception e ) {
                        PrometheusResult failed = new PrometheusResult("exception", null);
                        logger.error("Got exception for ["+entry.getKey()+"] - masked: " +e);
                        return Map.entry(entry.getKey(), failed);
                    }

                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
