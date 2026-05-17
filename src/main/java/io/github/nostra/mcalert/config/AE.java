package io.github.nostra.mcalert.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record AE (
    URI uri,
    Optional<String> datasource,
    Optional<List<AlertEndpointConfig.Header>> header,
    List<String> ignoreAlerts,
    List<String> watchdogAlerts
) implements AlertEndpointConfig.AlertEndpoint {
}
