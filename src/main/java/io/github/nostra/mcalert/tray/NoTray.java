package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.StatusWindow;
import io.github.nostra.mcalert.client.AlertResource;
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

    public Semaphore start() {
        logger.info("Starting GUI...");
        try {
            mutex.acquire();

            StatusWindow.blockUntilStarted();
            StatusWindow.getInstance().show(alertResource);

        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

        //alertResource.addPropertyChangeListener(this);
        // sets up the tray icon (using awt code run on the swing thread).
        return mutex;
    }

}
