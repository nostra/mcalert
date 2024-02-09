package io.github.nostra.mcalert.client;

import io.smallrye.config.ConfigMapping;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@ConfigMapping(prefix = "mcalert.prometheus")
public interface AlertEndpointConfig {
    Map<String, AlertEndpoint> endpoints();

    interface AlertEndpoint {
        URI uri();

        Optional<String> auth();
    }
}
