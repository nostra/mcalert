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
import io.quarkus.runtime.Shutdown;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class AlertResource {
    private static final Logger logger = LoggerFactory.getLogger(AlertResource.class);
    private final AlertEndpointConfig alertEndpointConfig;

    private Map<String, SingleEndpointPoller> alertEndpointMap;
    private Map<String, EndpointCallEnum> previousResult = new HashMap<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private boolean running = false;

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
            running = true;
        } catch (Exception e) {
            logger.error("When trying to read grafana datasource, an error occurred. Masked it is: "+ e.getMessage(), e);
        }
    }

    private Map<String, SingleEndpointPoller> createClientMap() {
        return alertEndpointConfig.endpoints()
                .entrySet()
                .stream()
                .filter(entry -> isDatasourceEmpty(entry.getValue().datasource()))
                .map(entry -> Map.entry(entry.getKey(), new SingleEndpointPoller(entry.getKey(), entry.getValue())))
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
                if ( entry == null || entry.getValue() == null ) {
                    throw new RuntimeException("Null for entry-value. Entry as such is "+entry);
                }
                var prom = entry.getValue().callPrometheus(entry.getKey());
                if ( prom.status().equalsIgnoreCase("success") && prom.noAlerts()) {
                    if ( status == null ) {
                        status = SUCCESS;
                    }
                } else if ( status == null || status == SUCCESS ) {
                    // Only set status = failure if it did not have a prior error in order not to mask reason
                    status = FAILURE;
                }
            } catch (NullPointerException e) {
                logger.error("Bah - got a NPE, please fix", e );
                status = FAILURE;
            } catch (Exception e) {
                logger.info("Trouble calling prometheus. Masked exception ({}) is {}", e.getClass().getSimpleName(), e.getMessage());
                if (e.getCause() instanceof ConnectException) {
                    status = OFFLINE;
                } else if (e.getCause() instanceof ServiceUnavailableException) {
                    status = OFFLINE;
                } else if (e.getCause() instanceof UnknownHostException) {
                    status = OFFLINE;
                } else if (e.getCause() instanceof NotAllowedException || e.getCause() instanceof NotAuthorizedException) {
                    status = NO_ACCESS;
                } else if (e.getCause() instanceof WebApplicationException cause) {
                    switch ( cause.getResponse().getStatus() ) {
                        case 401, 403 -> status = NO_ACCESS;
                        case 404 -> status = FOUR_O_FOUR;
                        case 503 -> status = OFFLINE;
                        default -> {
                            status = UNKNOWN_FAILURE;
                            logger.debug("Might need to create an icon for status {} which gave masked exception: {}",
                                    cause.getResponse().getStatus(), cause.getLocalizedMessage());
                        }
                    }
                } else {
                    logger.warn("Unmapped problem. Exception class: {} and cause {}", e.getClass().getName(), e.getCause().getClass().getName());
                    status = UNKNOWN_FAILURE;
                }
            }
        }
        EndpointCallEnum collatedStatus = alertEndpointMap.entrySet().stream().noneMatch(p -> p.getValue().isActive())
                ? ALL_DEACTIVATED
                : status;
        fireResult( "statusChange", status );
        return collatedStatus;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    private void fireResult( String key, EndpointCallEnum status) {
        EndpointCallEnum previous = previousResult.getOrDefault(key, OFFLINE);
        if ( previous != status ) {
            previousResult.put(key, status);
            logger.info("To trigger property change for {}/{}", key, status);
            PropertyChangeEvent event = new PropertyChangeEvent(this, key, previous, status);
            pcs.firePropertyChange(event);
        }
    }

    public Map<String, SingleEndpointPoller> map() {
        return alertEndpointMap;
    }

    public void toggle(String key) {
        SingleEndpointPoller sep = alertEndpointMap.get(key);
        sep.toggleActive();
        sep.outputStatus();
    }


    @Shutdown
    void shutdown() {
        logger.info("Shutdown-hook triggered");
        running = false;
    }

    /**
     * Call Prometheus endpoint and update the icon accordingly
     */
    @Scheduled( every = "${scheduledRefresh.every:60s}")
    void scheduledRefresh() {
        try {
            if ( running ) {
                fireAndGetCollatedStatus();
            } else {
                logger.info("Marked no running, therefore not refreshing");
            }
        } catch (Exception e) {
            logger.error("Error refreshing icon. Masked: {}", e.getMessage());
        }
    }

}
