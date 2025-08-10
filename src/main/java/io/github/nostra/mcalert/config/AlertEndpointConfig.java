package io.github.nostra.mcalert.config;

import io.smallrye.config.ConfigMapping;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "mcalert.prometheus")
public interface AlertEndpointConfig {
    Map<String, AlertEndpoint> endpoints();
    Optional<CommandLine> commandLine();

    interface AlertEndpoint {
        URI uri();

        /// If this endpoint is a grafana proxy, what is the name of the
        /// prometheus datasource?
        Optional<String> datasource();

        Optional<List<Header>> header();

        List<String> ignoreAlerts();

        List<String> watchdogAlerts();
    }

    interface Header {
        String name();
        String content();
    }

    interface CommandLine {
        String shellCommand();
    }
}
