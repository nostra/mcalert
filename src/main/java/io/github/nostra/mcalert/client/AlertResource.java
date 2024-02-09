package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.exception.McException;
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

@ApplicationScoped
public class AlertResource {
    private static final Logger logger = LoggerFactory.getLogger(AlertResource.class);

    AlertService alertService;

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
        alertService = createClient();
        clearListIfDisabled(namesToIgnore);
        clearListIfDisabled(watchdogAlertNames);
    }

    public AlertService createClient() {
        if ( alertEndpointConfig.endpoints().size() != 1) {
            throw new McException("Supporting more than 1 config is WIP");
        }

        return RestClientBuilder.newBuilder()
                //.baseUri(new URI("http://localhost:9090/api/v1/alerts"))
                .baseUri(alertEndpointConfig.endpoints().getFirst().uri())
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build(AlertService.class);
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
