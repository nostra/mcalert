package io.github.nostra.mcalert;

import io.github.nostra.mcalert.tray.NoTray;
import io.github.nostra.mcalert.tray.PrometheusTray;
import io.quarkus.runtime.Shutdown;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

@Dependent
public class McTrayService {
    private static final Logger log = LoggerFactory.getLogger(McTrayService.class);

    private PrometheusTray prometheusTray;
    private final NoTray noTray;

    @Inject
    public McTrayService(PrometheusTray prometheusTray, NoTray noTray ) {
        this.prometheusTray = prometheusTray;
        this.noTray = noTray;
    }

    Semaphore startTray() {
        log.info("Executing McService");
        return prometheusTray.start();
    }

    Semaphore startWithoutTray() {
        return noTray.start();
    }

    @Shutdown
    void shutdown() {
        log.info("Shutdown-hook triggering (McService)");
    }
}
