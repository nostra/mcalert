package io.github.nostra.mcalert;

import io.github.nostra.mcalert.tray.PrometheusTray;
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

    private PrometheusTray prometheusTray;

    @Inject
    public McService(PrometheusTray prometheusTray) {
        this.prometheusTray = prometheusTray;
    }

    Semaphore execute() {
        log.info("Executing McService");
        return prometheusTray.start();
    }

    @Shutdown
    void shutdown() {
        logger.info("Shutdown-hook triggering (McService)");
    }
}
