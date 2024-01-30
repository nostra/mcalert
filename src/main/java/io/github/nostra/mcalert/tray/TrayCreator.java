package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.exception.McException;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Shutdown;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Semaphore;

@Singleton
public class TrayCreator  {
    private static final Logger logger = LoggerFactory.getLogger(TrayCreator.class);
    private TrayIcon trayIcon ;
    private Image okImage;
    private Image failureImage;
    private Semaphore mutex = new Semaphore(1);

    /*
    private final AlertService alertService;

    @Inject
    public TrayCreator(@RestClient AlertService alertService) {
        this.alertService = alertService;
    }
*/
    public Semaphore start() {
        logger.info("Starting GUI...");

        try {
            mutex.acquire();
            okImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/arrow-up-circle-fill.png")));
            failureImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/arrow-down-double-line.png")));
        } catch (IOException e) {
            throw new McException("Could not initialize", e);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }

        // sets up the tray icon (using awt code run on the swing thread).
        SwingUtilities.invokeLater(this::addIconToTray);
        return mutex;
    }

    @Shutdown
    void shutdown() {
        logger.info("Shutdown-hook triggering (TrayCreator)");
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
        trayIcon = new TrayIcon(okImage);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> SwingUtilities.invokeLater(()-> logger.info("Action listener triggered")));
        trayIcon.addActionListener(event -> SwingUtilities.invokeLater(this::refresh));

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
            logger.debug("Menuitem triggered, refreshing");
            refresh();
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

        refreshItem.addActionListener(e -> logger.info("Some action on refreshItem"));
        return popup;
    }

    private void refresh() {
        //logger.info("Call to alert service gave " + alertService.getResult());
        final var imageToSet =
            trayIcon.getImage() == okImage
            ? failureImage
            : okImage;
        SwingUtilities.invokeLater( () -> trayIcon.setImage(imageToSet));
    }
}
