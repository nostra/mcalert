package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.StatusWindow;
import io.github.nostra.mcalert.client.AlertResource;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Shutdown;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

@Singleton
public class NoTray {
    private final Semaphore mutex = new Semaphore(1);
    private final AlertResource alertResource;
    private static final Logger logger = LoggerFactory.getLogger(NoTray.class);

    @Inject
    public NoTray( AlertResource alertResource) {
        this.alertResource = alertResource;
    }

    @Shutdown
    void shutdown() {
        logger.info("Shutdown-hook triggering (NoTray)");

        mutex.release();
        Quarkus.asyncExit();
    }

    public Semaphore start() {
        logger.info("Starting GUI...");
        StatusWindow.blockUntilStarted();
        mutex.acquireUninterruptibly();


        StatusWindow.getInstance().show(alertResource);

        return mutex;
    }
}
