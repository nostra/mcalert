module io.github.nostra.mcalert {
    requires com.fasterxml.jackson.annotation;
    requires info.picocli;
    requires jakarta.cdi;
    requires jakarta.ws.rs;
    requires java.desktop;
    requires jdk.httpserver; // For testing
    requires microprofile.config.api;
    requires microprofile.rest.client.api;
    requires org.slf4j;
    requires quarkus.core;
    requires quarkus.scheduler.api;
    requires smallrye.config.core;

    opens io.github.nostra.mcalert;

    exports io.github.nostra.mcalert.client;
    exports io.github.nostra.mcalert.config;
    exports io.github.nostra.mcalert.model;
    exports io.github.nostra.mcalert.tray;
    exports io.github.nostra.mcalert;
}