package io.github.nostra.mcalert.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record AEConfig(
    @JsonDeserialize(contentAs = AE.class)
    Map<String, AlertEndpointConfig.AlertEndpoint> endpoints,
    @JsonDeserialize(contentAs = AeCommandLine.class)
    Optional<CommandLine> commandLine,
    Optional<Boolean> darkmode
) implements AlertEndpointConfig {
}
