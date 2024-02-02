package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.AlertModel;
import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@ApplicationScoped
public class AlertResource {
    private static final Logger logger = LoggerFactory.getLogger(AlertResource.class);

    @RestClient
    AlertService alertService;

    @ConfigProperty(name = "prometheus.ignore.alerts")
    List<String> namesToIgnore;

    /**
     * Watchdog alerts are alerts that should fire, they don't there is an error.
     */
    @ConfigProperty(name = "prometheus.watchdog.alerts")
    List<String> watchdogAlertNames;

    @PostConstruct
    void init() {
        clearListIfDisabled(namesToIgnore);
        clearListIfDisabled(watchdogAlertNames);
    }

    /**
     * @return data structure with only firing and relevant alerts. Ensure presence
     * of watchdog alert
     */
    public PrometheusResult getFiringAndRelevant() {
        PrometheusResult result = alertService.getResult();
        List<AlertModel> toRemove = extractIrrelevantAlerts(result);
        result.data().alerts().removeAll(toRemove);
        if (result.data().alerts().isEmpty()) {
            // Ensure that watchdog alerts are present if nothing else fires
            result.data()
                    .alerts()
                    .addAll(findMissingWatchDogAlerts(toRemove));
        }
        return result;
    }


    /**
     * Quarkus does not let us easily configure empty list. Having a entry which
     * flags emptyness fixes this
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
