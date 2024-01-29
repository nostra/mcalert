package io.github.nostra.mcalert;

import io.github.nostra.mcalert.exception.McException;
import io.github.nostra.mcalert.tray.TrayCreator;
import io.quarkus.runtime.Shutdown;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

@Dependent
public class McService {
    private final Logger log = LoggerFactory.getLogger(McService.class);
    private static final Logger logger = LoggerFactory.getLogger(McService.class);

    private TrayCreator trayCreator;

    @Inject
    public McService(TrayCreator trayCreator) {
        this.trayCreator = trayCreator;
    }

    Semaphore execute() {
        log.info("Executing McService");
        return trayCreator.start();
    }

    @Shutdown
    void shutdown() {
        logger.info("Shutdown-hook triggering (McService)");
    }

    public void loadConfig(String config) {
        throw new McException("Not implemented yet, won't load "+config);
    }
}
