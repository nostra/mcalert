package io.github.nostra.mcalert.config;

import java.util.Map;
import java.util.Optional;

public class AlertEndpointConfigHolder implements AlertEndpointConfig {
    @Override
    public Map<String, AlertEndpoint> endpoints() {
        return Map.of();
    }

    @Override
    public Optional<CommandLine> commandLine() {
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> darkmode() {
        return Optional.empty();
    }
}
