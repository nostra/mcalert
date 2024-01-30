module io.github.nostra.mcalert {
    requires org.slf4j;
    requires java.desktop;
    requires jakarta.cdi;
    requires quarkus.core;
    requires info.picocli;
    requires jakarta.ws.rs;
    requires microprofile.rest.client.api;

    opens io.github.nostra.mcalert;
    exports io.github.nostra.mcalert;
    exports io.github.nostra.mcalert.model;
}