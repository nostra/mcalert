package io.github.nostra.mcalert;

import io.github.nostra.mcalert.tray.NoTray;
import io.github.nostra.mcalert.tray.PrometheusTray;
import io.github.nostra.mcalert.tray.ShellCommandListener;
import io.quarkus.runtime.Shutdown;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

@Dependent
public class McTrayService {
    private static final Logger log = LoggerFactory.getLogger(McTrayService.class);

    private final PrometheusTray prometheusTray;
    private final NoTray noTray;
    private final ShellCommandListener cli;

    @Inject
    public McTrayService(PrometheusTray prometheusTray, NoTray noTray, ShellCommandListener cli) {
        this.prometheusTray = prometheusTray;
        this.noTray = noTray;
        this.cli = cli;
    }

    private Semaphore startTray() {
        log.info("Executing McService");
        return prometheusTray.start();
    }

    private Semaphore startWithoutTray() {
        return noTray.start();
    }

    @Shutdown
    void shutdown() {
        log.info("Shutdown-hook triggering (McService)");
    }

    public Semaphore startServices(boolean noTray) {
        if ( noTray) {
            log.info("System tray support disabled by --no-tray argument or system variable.");
            log.warn("Currently exiting, later do something useful");
            return startWithoutTray();
        } else {
            log.info("System tray support enabled.");
            return startTray();
        }
    }
}
