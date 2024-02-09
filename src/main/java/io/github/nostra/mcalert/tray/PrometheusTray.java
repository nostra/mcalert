package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.client.AlertResource;
import io.github.nostra.mcalert.exception.McException;
import io.github.nostra.mcalert.model.PrometheusResult;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Shutdown;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;
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
    private final Semaphore mutex = new Semaphore(1);
    private boolean running = false;

    private AlertResource alertResource;

    @Inject
    public PrometheusTray( AlertResource alertResource) {
        this.alertResource = alertResource;
    }

    public Semaphore start() {
        logger.info("Starting GUI...");
        try {
            mutex.acquire();
            okImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/pulse-line.png")));
            circleImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/circle-line.png")));
            failureImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/bug-line.png")));
            offlineImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/cloud-off-fill.png")));
            noAccessImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/prohibited-line.png")));

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
        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.remove(trayIcon);
        } catch (Exception e) {
            logger.error("Trouble cleaning up....", e);
        }
    }


    private void addIconToTray() {
        trayIcon = new TrayIcon(circleImage);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> SwingUtilities.invokeLater(this::callAndRefreshIcon));
        trayIcon.setPopupMenu(constructTrayMenu());

        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (Exception e) {
            logger.error("Trouble setting up....", e);
        }
    }

    private PopupMenu constructTrayMenu() {
        MenuItem refreshItem = new MenuItem("refresh");
        refreshItem.addActionListener(e -> {
            logger.debug("Menuitem triggered, force refresh");
            callAndRefreshIcon();
        });
        MenuItem exitItem = new MenuItem("exit");
        exitItem.addActionListener(e -> {
            logger.info("Exit chosen, platform exit");
            mutex.release();
            Quarkus.asyncExit();
        });

        PopupMenu popup = new PopupMenu();
        popup.add(refreshItem);
        popup.add(exitItem);

        return popup;
    }

    /**
     * Call Prometheus endpoint and update the icon accordingly
     */
     @Scheduled( every = "${scheduledRefresh.every.expr:60s}")
     void scheduledRefresh() {
         if ( running ) {
             callAndRefreshIcon();
         }
     }

     void callAndRefreshIcon() {
        try {
            var prom = alertResource.getFiringAndRelevant();
            /*
            if ( prom.stream().filter(p -> p.noAlerts() == false ).count() == 0) {
                logger.trace("Got prom with: " + prom.debugOutput());
            } else {
                logger.debug("Got alerts: "+prom.data().alerts());
            }*/
            var numSuccessful = prom.entrySet().stream()
                    .filter(p -> p.getValue().status().equalsIgnoreCase("success"))
                    .map( p -> {
                        if (p.getValue().noAlerts()) {
                            logger.debug("Got prom["+p.getKey()+"] with: " + p.getValue().debugOutput());
                        } else {
                            logger.debug("Got alerts["+p.getKey()+"]: "+p.getValue().data().alerts());
                        }
                        return p;
                    })
                    .map(Map.Entry::getValue)
                    .filter(PrometheusResult::noAlerts)
                    .count();
            final var imageToSet =
                    numSuccessful == prom.size()
                            ? okImage
                            : failureImage;
            SwingUtilities.invokeLater(() -> trayIcon.setImage(imageToSet));
        } catch (Exception e) {
            logger.info("Trouble calling prometheus. Masked exception is " + e.getMessage());
            if (e.getCause() instanceof ConnectException) {
                SwingUtilities.invokeLater(() -> trayIcon.setImage(offlineImage));
            } else if (e.getCause() instanceof NotAllowedException || e.getCause() instanceof NotAuthorizedException ) {
                SwingUtilities.invokeLater(() -> trayIcon.setImage(noAccessImage));
            } else {
                SwingUtilities.invokeLater(() -> trayIcon.setImage(failureImage));
            }
        }
    }
}
