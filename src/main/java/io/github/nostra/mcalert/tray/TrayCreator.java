package io.github.nostra.mcalert.tray;

import io.quarkus.runtime.Quarkus;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TrayCreator extends Application {
    private static final Logger logger = LoggerFactory.getLogger(TrayCreator.class);
    private Stage stage;
    private TrayIcon trayIcon ;
    private Image okImage;
    private Image failureImage;

    public static void boot() {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        logger.info("Starting javafx...");
        this.stage = stage;
        // instructs the javafx system not to exit implicitly when the last application window is shut.
        Platform.setImplicitExit(false);
        okImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/arrow-up-circle-fill.png")));
        failureImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/arrow-down-double-line.png")));

        // sets up the tray icon (using awt code run on the swing thread).
        SwingUtilities.invokeLater(this::addAppToTray);
    }

    private void addAppToTray() {
        trayIcon = new TrayIcon(okImage);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(event -> Platform.runLater(()->{logger.info("Action listener triggered");}));
        trayIcon.addActionListener(event -> Platform.runLater(this::refresh));

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
            Quarkus.asyncExit();
            Platform.exit();
        });

        PopupMenu popup = new PopupMenu();
        popup.add(refreshItem);
        popup.add(exitItem);

        refreshItem.addActionListener(e -> logger.info("Some action on refreshItem"));
        return popup;
    }

    private void refresh() {
        final var imageToSet =
            trayIcon.getImage() == okImage
            ? failureImage
            : okImage;
        SwingUtilities.invokeLater( () -> trayIcon.setImage(imageToSet));
    }
}
