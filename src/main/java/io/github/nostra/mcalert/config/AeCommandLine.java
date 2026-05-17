package io.github.nostra.mcalert.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record AeCommandLine(
    String shellCommand
) implements AlertEndpointConfig.CommandLine {
}
