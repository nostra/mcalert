package io.github.nostra.mcalert.config;

public record AEHeader(
    String name,
    String content
) implements AlertEndpointConfig.Header{
}
