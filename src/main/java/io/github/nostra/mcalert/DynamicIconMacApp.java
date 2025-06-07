package io.github.nostra.mcalert;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DynamicIconMacApp extends Application {

    private int iconState = 0;
    private final boolean isMac = System.getProperty("os.name", "").toLowerCase().startsWith("mac");

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Dynamic Icon");
        primaryStage.setScene(new Scene(new StackPane(), 300, 250));

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            updateIcon(primaryStage);
            iconState = (iconState + 1) % 4;
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);

        primaryStage.setOnShown(event -> timeline.play());
        primaryStage.setOnCloseRequest(event -> timeline.stop());

        // Set initial icon
        updateIcon(primaryStage);

        primaryStage.show();
    }

    private void updateIcon(Stage stage) {
        Image fxImage = createIcon(iconState);

        // For macOS, use the AWT Taskbar API to set the dock icon.
        if (isMac && Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                // The AWT update must not happen on the FX Application Thread.
                // It's a lightweight operation, so Platform.runLater is not strictly
                // needed here, but it's good practice for any UI-related off-thread work.
                BufferedImage awtImage = SwingFXUtils.fromFXImage(fxImage, null);
                taskbar.setIconImage(awtImage);
            }
        } else {
            // For Windows/Linux, the standard JavaFX way works.
            stage.getIcons().setAll(fxImage);
        }
    }

    private Image createIcon(int state) {
        Canvas canvas = new Canvas(128, 128); // Use a higher resolution for better quality
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.CORNFLOWERBLUE);
        gc.fillRoundRect(0, 0, 128, 128, 25, 25);
        gc.setFill(Color.WHITE);

        gc.save();
        gc.translate(64, 64);
        gc.rotate(state * 90);
        gc.fillRect(-20, -40, 40, 80);
        gc.restore();

        return canvas.snapshot(null, null);
    }

    public static void main(String[] args) {
        // Required for AWT/Swing/JavaFX interoperability.
        // This prevents the JVM from exiting when the last JavaFX window is closed.
        // We will manage the exit with Platform.exit() in the close request.
        Platform.setImplicitExit(false);
        launch(args);
    }
}