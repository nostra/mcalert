package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.config.AlertEndpointConfig;
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
                .filter(entry -> (entry.getValue().isGrafana().orElse(Boolean.FALSE)))
                .map(entry -> Map.entry(entry.getKey(), new SingleEndpointPoller(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .entrySet()
                .stream()
                .map(entry ->
                        entry.getValue().callGrafanaForDs()
                                .stream()
                                .filter(d -> d.name().equals(entry.getKey()))
                                .findFirst()
                                .orElse(null)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        GrafanaDatasource::name,
                        grafanaDatasource -> {
                            AlertEndpointConfig.AlertEndpoint ep = grafanaDatasourceConfig.endpoints().get(grafanaDatasource.name());
                            AlertEndpointConfig.AlertEndpoint endpoint = new GrafanaAlertEndpoint(ep, grafanaDatasource);

                            return new SingleEndpointPoller(endpoint);
                        }
                ));
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
        /// from a grafana datasource. Thus, it shall answer false on whether it is a grafana endpoint.
        @Override
        public Optional<Boolean> isGrafana() {
            return Optional.of(Boolean.FALSE);
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
