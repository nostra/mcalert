package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.client.AlertResource;
import io.github.nostra.mcalert.exception.McException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Shutdown;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Semaphore;

@Singleton
public class PrometheusTray {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusTray.class);
    private TrayIcon trayIcon ;
    private Image okImage;
    private Image circleImage;
    private Image failureImage;
    private Image offlineImage;
    private Image noAccessImage;
    private Image deactivatedImage;
    private final Semaphore mutex = new Semaphore(1);
    private boolean running = false;

    private final AlertResource alertResource;

    @Inject
    public PrometheusTray( AlertResource alertResource) {
        this.alertResource = alertResource;
    }

    public Semaphore start() {
        logger.info("Starting GUI...");
        try {
            mutex.acquire();
            okImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/pulse-line.png")));
            circleImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/circle-line.png")));
            failureImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/bug-line.png")));
            offlineImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/cloud-off-fill.png")));
            noAccessImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/prohibited-line.png")));
            deactivatedImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/images/information-off-line.png")));

        } catch (IOException e) {
            throw new McException("Could not initialize", e);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }

        // sets up the tray icon (using awt code run on the swing thread).
        SwingUtilities.invokeLater(this::addIconToTray);
        running = true;
        return mutex;
    }

    @Shutdown
    void shutdown() {
        logger.info("Shutdown-hook triggered");
        running = true;
        SwingUtilities.invokeLater(this::removeIconFromTray);
    }

    private void removeIconFromTray() {
        try {
            SystemTray tray = SystemTray.getSystemTray();
            tray.remove(trayIcon);
        } catch (UnsupportedOperationException ignore) {
            // ignore
        } catch (Exception e) {
            logger.error("Trouble cleaning up....", e);
        }
    }


    private void addIconToTray() {
        trayIcon = new TrayIcon(circleImage);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> SwingUtilities.invokeLater(this::callAndRefreshIcon));
        trayIcon.setPopupMenu(constructTrayMenu());
        trayIcon.setToolTip("McAlert");

        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (Exception e) {
            logger.error("Trouble setting up....", e);
        }
    }

    private PopupMenu constructTrayMenu() {
        var menuItems = new ArrayList<MenuItem>();
        MenuItem refreshItem = new MenuItem("refresh");
        refreshItem.addActionListener(e -> {
            logger.debug("Menuitem triggered, force refresh");
            callAndRefreshIcon();
        });
        menuItems.add( refreshItem );
        alertResource.map().forEach((key, value) -> {
            AlertMenuItem item = new AlertMenuItem(key);
            value.addPropertyChangeListener(item);
            item.addActionListener(e -> alertResource.toggle( key ));
            menuItems.add(item);
        });
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            logger.info("Exit chosen, platform exit");
            mutex.release();
            Quarkus.asyncExit();
        });
        menuItems.add(exitItem);

        PopupMenu popup = new PopupMenu();
        menuItems.forEach(popup::add);

        return popup;
    }

    /**
     * Call Prometheus endpoint and update the icon accordingly
     */
     @Scheduled( every = "${scheduledRefresh.every:60s}")
     void scheduledRefresh() {
         try {
             if ( running ) {
                 callAndRefreshIcon();
             }
         } catch (Exception e) {
             logger.error("Error refreshing icon. Masked: {}", e.getMessage());
         }
     }

     void callAndRefreshIcon() {
        var status = alertResource.fireAndGetCollatedStatus();
        switch (status) {
            case EMPTY -> logger.warn("Configuration error: No endpoints configured");
            case FOUR_O_FOUR, OFFLINE -> SwingUtilities.invokeLater(() -> trayIcon.setImage(offlineImage));
            case SUCCESS -> SwingUtilities.invokeLater(() -> trayIcon.setImage(okImage));
            case NO_ACCESS -> SwingUtilities.invokeLater(() -> trayIcon.setImage(noAccessImage));
            case ALL_DEACTIVATED -> SwingUtilities.invokeLater(() -> trayIcon.setImage(deactivatedImage));
            case UNKNOWN_FAILURE, FAILURE -> SwingUtilities.invokeLater(() -> trayIcon.setImage(failureImage));
        }
    }
}
