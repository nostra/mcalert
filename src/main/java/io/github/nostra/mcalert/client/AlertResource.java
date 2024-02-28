package io.github.nostra.mcalert.client;

import static io.github.nostra.mcalert.client.EndpointCallEnum.ALL_DEACTIVATED;
import static io.github.nostra.mcalert.client.EndpointCallEnum.EMPTY;
import static io.github.nostra.mcalert.client.EndpointCallEnum.FAILURE;
import static io.github.nostra.mcalert.client.EndpointCallEnum.NO_ACCESS;
import static io.github.nostra.mcalert.client.EndpointCallEnum.OFFLINE;
import static io.github.nostra.mcalert.client.EndpointCallEnum.SUCCESS;
import static io.github.nostra.mcalert.client.EndpointCallEnum.UNKNOWN_FAILURE;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class AlertResource {
    private static final Logger logger = LoggerFactory.getLogger(AlertResource.class);
    private final AlertEndpointConfig alertEndpointConfig;

    Map<String, SingleEndpointPoller> alertEndpointMap;

    public AlertResource(AlertEndpointConfig alertEndpointConfig) {
        this.alertEndpointConfig = alertEndpointConfig;
    }

    @PostConstruct
    void init() {
        alertEndpointMap = createClientMap();
    }

    private Map<String, SingleEndpointPoller> createClientMap() {
        return alertEndpointConfig.endpoints()
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), new SingleEndpointPoller(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
                logger.info("Trouble calling prometheus. Masked exception is " + e.getMessage());
                if (e.getCause() instanceof ConnectException) {
                    status = OFFLINE;
                } else if (e.getCause() instanceof NotAllowedException || e.getCause() instanceof NotAuthorizedException) {
                    status = NO_ACCESS;
                } else if (e.getCause() instanceof WebApplicationException) {
                    WebApplicationException cause = (WebApplicationException) e.getCause();
                    switch ( cause.getResponse().getStatus() ) {
                        case 401, 403 -> status = NO_ACCESS;
                        case 404 -> status = OFFLINE;
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
        alertEndpointMap.get(key).toggleActive();
    }
}
