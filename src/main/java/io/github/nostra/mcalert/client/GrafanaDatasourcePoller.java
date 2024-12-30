package io.github.nostra.mcalert.client;

import static io.github.nostra.mcalert.client.AlertResource.isDatasourceEmpty;
import io.github.nostra.mcalert.config.AlertEndpointConfig;
import io.github.nostra.mcalert.exception.McConfigurationException;
import io.github.nostra.mcalert.model.GrafanaDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class GrafanaDatasourcePoller {
    private static final Logger log = LoggerFactory.getLogger(GrafanaDatasourcePoller.class);
    private final AlertEndpointConfig grafanaDatasourceConfig;
    private Map<String, SingleEndpointPoller> grafanaEndpoints = new HashMap<>();

    public GrafanaDatasourcePoller(AlertEndpointConfig grafanaDatasourceConfig) {
        this.grafanaDatasourceConfig = grafanaDatasourceConfig;
    }


    private void refreshEndpoints() {
        grafanaEndpoints = grafanaDatasourceConfig
                .endpoints()
                .entrySet()
                .stream()
                .filter(entry -> !isDatasourceEmpty(entry.getValue().datasource()))
                .map(entry -> {
                    var ep = findEpFromDs(entry.getValue());
                    return ep == null
                            ? null
                            : Map.entry(entry.getKey(), ep);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private SingleEndpointPoller findEpFromDs(AlertEndpointConfig.AlertEndpoint alertEndpoint) {
        List<SingleEndpointPoller> list = new SingleEndpointPoller(alertEndpoint)
                .callGrafanaForDs()
                .stream()
                .filter(d -> d.name().equals(alertEndpoint.datasource().orElseThrow(() -> new McConfigurationException("Unexpected use in method"))))
                .map(ds -> new GrafanaAlertEndpoint(alertEndpoint, ds))
                .map(SingleEndpointPoller::new)
                .toList();

        if (list.size() != 1) {
            log.error("Expected to find exactly one datasource name \"{}\" with uri {}, but found {}.",
                    alertEndpoint.datasource().orElse("not configured"),  alertEndpoint.uri(), list.size());
        }
        return list.isEmpty()
                ? null
                : list.getFirst();
    }


    public Map<String, SingleEndpointPoller> startPolling() {
        // TODO Update after read failure (or n number of reads), to try to fetch updated datasource
        refreshEndpoints();
        return grafanaEndpoints;
    }

    private static class GrafanaAlertEndpoint implements AlertEndpointConfig.AlertEndpoint {
        private final AlertEndpointConfig.AlertEndpoint ep;
        private final GrafanaDatasource grafanaDatasource;

        public GrafanaAlertEndpoint(AlertEndpointConfig.AlertEndpoint ep, GrafanaDatasource grafanaDatasource) {
            if ( ep == null || grafanaDatasource == null ) {
                throw new McConfigurationException("Endpoint or datasource is null");
            }
            this.ep = ep;
            this.grafanaDatasource = grafanaDatasource;
        }

        @Override
        public URI uri() {
            URI uri = URI.create(ep.uri().toString() + "/uid/" + grafanaDatasource.uid() + "/resources/api/v1/alerts?state=firing");
            log.info("Prometheus endpoint by grafana proxy: {}", uri);
            return uri;
        }

        /// This class is dynamically created, and represents a prometheus endpoint read
        /// from a grafana datasource. This is the datasource name.
        @Override
        public Optional<String> datasource() {
            return ep.datasource();
        }

        @Override
        public Optional<List<AlertEndpointConfig.Header>> header() {
            return ep.header();
        }

        @Override
        public List<String> ignoreAlerts() {
            return ep.ignoreAlerts();
        }

        @Override
        public List<String> watchdogAlerts() {
            return ep.watchdogAlerts();
        }
    }
}
