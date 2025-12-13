package io.github.nostra.mcalert.splash;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public final class SplashStage extends Stage {

    public SplashStage() {
        initStyle(StageStyle.TRANSPARENT);
        setAlwaysOnTop(true);

        Image image = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/images/splash.png"),
                "Splash image not found at /images/splash.png"
        ));

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(420);

        VBox root = new VBox(10, imageView, new Text("Loading…"));
        root.setPadding(new Insets(12));
        root.setBackground(Background.EMPTY);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);

        sizeToScene();
        centerOnScreen();

        // Listen for the "close splash" event:
        SplashEventBus.closeRequestedProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                close();
            }
        });
    }
}