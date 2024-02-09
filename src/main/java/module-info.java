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

    opens io.github.nostra.mcalert;
    exports io.github.nostra.mcalert;
    exports io.github.nostra.mcalert.model;
}