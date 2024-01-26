package io.github.nostra.mcalert.tray;

import io.quarkus.runtime.Shutdown;
import jakarta.enterprise.context.ApplicationScoped;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShutdownJavaFX {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownJavaFX.class);

    @Shutdown
    void shutdown() {
        logger.info("Shutting down JavaFX as application has stopped");
        Platform.exit();
    }

}
