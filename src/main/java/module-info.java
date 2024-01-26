module io.github.nostra.mcalert {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;

    requires java.desktop;
    requires jakarta.cdi;
    requires quarkus.core;
    requires info.picocli;

    opens io.github.nostra.mcalert;
    exports io.github.nostra.mcalert;
}