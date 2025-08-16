package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.StatusWindow;
import io.github.nostra.mcalert.client.AlertResource;
import io.github.nostra.mcalert.client.EndpointCallEnum;
import io.github.nostra.mcalert.exception.McException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Shutdown;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Semaphore;

@Singleton
public class PrometheusTray implements PropertyChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusTray.class);
    private TrayIcon trayIcon ;
    private Image okImage;
    private Image circleImage;
    private Image failureImage;
    private Image offlineImage;
    private Image noAccessImage;
    private Image deactivatedImage;
    private final Semaphore mutex = new Semaphore(1);

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
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

        alertResource.addPropertyChangeListener(this);
        // sets up the tray icon (using awt code run on the swing thread).
        SwingUtilities.invokeLater(this::addIconToTray);
        return mutex;
    }

    @Shutdown
    void shutdown() {
        logger.info("Shutdown-hook triggered");
        alertResource.removePropertyChangeListener(this);
        SwingUtilities.invokeLater(this::removeIconFromTray);
    }

    private void removeIconFromTray() {
        try {
            SystemTray tray = SystemTray.getSystemTray();
            tray.remove(trayIcon);
        } catch (UnsupportedOperationException _) {
            // ignore
        } catch (Exception e) {
            logger.error("Trouble cleaning up....", e);
        }
    }


    private void addIconToTray() {
        trayIcon = new TrayIcon(circleImage);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(_ -> SwingUtilities.invokeLater(() -> alertResource.fireAndGetCollatedStatus()));
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
        MenuItem detailWindow = new MenuItem("Show Window");
        detailWindow.addActionListener(_ -> Platform.runLater(() -> {
            if ( StatusWindow.getInstance() == null) {
                logger.error("Whut - null statuswindow??");
            } else {
                StatusWindow.getInstance().show(alertResource);
            }
        }));
        menuItems.add(detailWindow);

        alertResource.map().forEach((key, value) -> {
            AlertMenuItem item = new AlertMenuItem(key);
            value.addPropertyChangeListener(item);
            item.addActionListener(e -> {
                logger.info("Event is: {}", e);
                alertResource.toggle( key );
            });
            menuItems.add(item);
        });

        var refreshItem = new MenuItem("refresh");
        refreshItem.addActionListener(e -> {
            logger.debug("Menuitem triggered, force refresh");
            refreshTrayIconWith(alertResource.fireAndGetCollatedStatus());
        });
        menuItems.add( refreshItem );

        var exitItem = new MenuItem("Exit");
        exitItem.addActionListener(_ -> {
            logger.info("Exit chosen, platform exit");
            mutex.release();
            Quarkus.asyncExit();
        });
        menuItems.add(exitItem);

        PopupMenu popup = new PopupMenu();
        menuItems.forEach(popup::add);

        return popup;
    }

     void refreshTrayIconWith(EndpointCallEnum status) {
        switch (status) {
            case EMPTY -> logger.warn("Configuration error: No endpoints configured");
            case FOUR_O_FOUR, OFFLINE -> SwingUtilities.invokeLater(() -> trayIcon.setImage(offlineImage));
            case SUCCESS -> SwingUtilities.invokeLater(() -> trayIcon.setImage(okImage));
            case NO_ACCESS -> SwingUtilities.invokeLater(() -> trayIcon.setImage(noAccessImage));
            case ALL_DEACTIVATED -> SwingUtilities.invokeLater(() -> trayIcon.setImage(deactivatedImage));
            case UNKNOWN_FAILURE, FAILURE -> SwingUtilities.invokeLater(() -> trayIcon.setImage(failureImage));
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ( "statusChange".equals(evt.getPropertyName())) {
            refreshTrayIconWith((EndpointCallEnum) evt.getNewValue());
        } else {
            logger.warn("Got unexpected event {}", evt.getPropertyName());
        }
    }
}
