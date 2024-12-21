module io.github.nostra.mcalert {
    requires org.slf4j;
    requires java.desktop;
    requires jakarta.cdi;
    requires quarkus.core;
    requires info.picocli;
    requires jakarta.ws.rs;
    requires microprofile.rest.client.api;
    requires microprofile.config.api;
    requires quarkus.scheduler.api;
    requires smallrye.config.core;
    requires com.fasterxml.jackson.annotation;

    opens io.github.nostra.mcalert;
    exports io.github.nostra.mcalert;
    exports io.github.nostra.mcalert.model;
    exports io.github.nostra.mcalert.tray;
    exports io.github.nostra.mcalert.client;
    exports io.github.nostra.mcalert.config;
}