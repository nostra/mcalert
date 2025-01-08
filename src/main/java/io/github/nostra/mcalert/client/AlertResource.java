package io.github.nostra.mcalert.client;

import static io.github.nostra.mcalert.client.EndpointCallEnum.ALL_DEACTIVATED;
import static io.github.nostra.mcalert.client.EndpointCallEnum.EMPTY;
import static io.github.nostra.mcalert.client.EndpointCallEnum.FAILURE;
import static io.github.nostra.mcalert.client.EndpointCallEnum.FOUR_O_FOUR;
import static io.github.nostra.mcalert.client.EndpointCallEnum.NO_ACCESS;
import static io.github.nostra.mcalert.client.EndpointCallEnum.OFFLINE;
import static io.github.nostra.mcalert.client.EndpointCallEnum.SUCCESS;
import static io.github.nostra.mcalert.client.EndpointCallEnum.UNKNOWN_FAILURE;
import io.github.nostra.mcalert.config.AlertEndpointConfig;
import io.github.nostra.mcalert.exception.McConfigurationException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class AlertResource {
    private static final Logger logger = LoggerFactory.getLogger(AlertResource.class);
    private final AlertEndpointConfig alertEndpointConfig;

    private Map<String, SingleEndpointPoller> alertEndpointMap;

    public AlertResource(AlertEndpointConfig alertEndpointConfig) {
        this.alertEndpointConfig = alertEndpointConfig;
    }

    @PostConstruct
    void init() {
        alertEndpointMap = createClientMap();
        GrafanaDatasourcePoller grafanaDatasourcePoller = new GrafanaDatasourcePoller(alertEndpointConfig);
        // TODO Would need to update the alertEndpointMap if the grafana datasource changes

        try {
            grafanaDatasourcePoller.startPolling()
                    .forEach((key, value) -> {
                        if (alertEndpointMap.put(key, value) != null) {
                            throw new McConfigurationException("Key '" + key + "' already exists. You need to rename the static endpoint.");
                        }
                    });
            logger.info("AlertResource initialized with {} endpoints", alertEndpointMap.size());
        } catch (Exception e) {
            logger.error("When trying to read grafana datasource, an error occurred. Masked it is: "+ e.getMessage(), e);
        }
    }

    private Map<String, SingleEndpointPoller> createClientMap() {
        return alertEndpointConfig.endpoints()
                .entrySet()
                .stream()
                .filter(entry -> isDatasourceEmpty(entry.getValue().datasource()))
                .map(entry -> Map.entry(entry.getKey(), new SingleEndpointPoller(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static boolean isDatasourceEmpty(Optional<String> datasource) {
        return datasource.isEmpty() || datasource.get().trim().isEmpty();
    }

    /**
     * @return One failure means all fail
     */
    public EndpointCallEnum fireAndGetCollatedStatus() {
        if (alertEndpointMap.isEmpty()) {
            logger.info("Please configure Prometheus endpoints");
            return EMPTY;
        }
        EndpointCallEnum status = null;
        for (var entry : alertEndpointMap.entrySet()) {
            try {
                var prom = entry.getValue().callPrometheus(entry.getKey());
                if ( prom.status().equalsIgnoreCase("success") && prom.noAlerts()) {
                    if ( status == null ) {
                        status = SUCCESS;
                    }
                } else {
                    status = FAILURE;
                }
            } catch (Exception e) {
                logger.info("Trouble calling prometheus. Masked exception is {}", e.getMessage());
                if (e.getCause() instanceof ConnectException) {
                    status = OFFLINE;
                } else if (e.getCause() instanceof NotAllowedException || e.getCause() instanceof NotAuthorizedException) {
                    status = NO_ACCESS;
                } else if (e.getCause() instanceof WebApplicationException cause) {
                    switch ( cause.getResponse().getStatus() ) {
                        case 401, 403 -> status = NO_ACCESS;
                        case 404 -> status = FOUR_O_FOUR;
                        default -> status = UNKNOWN_FAILURE;
                    }
                } else {
                    status = UNKNOWN_FAILURE;
                }
            }
        }
        return alertEndpointMap.entrySet().stream().noneMatch(p -> p.getValue().isActive())
                ? ALL_DEACTIVATED
                : status;
    }

    public Map<String, SingleEndpointPoller> map() {
        return alertEndpointMap;
    }

    public void toggle(String key) {
        SingleEndpointPoller sep = alertEndpointMap.get(key);
        sep.toggleActive();
        sep.outputStatus();
    }
}
