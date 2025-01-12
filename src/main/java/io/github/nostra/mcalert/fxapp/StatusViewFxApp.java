package io.github.nostra.mcalert.fxapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusViewFxApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(StatusViewFxApp.class);
    private Scene scene;
    private Stage stage;

    public void startFxApp() {
        launch();
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Web View");
        scene = new Scene(new Browser(),750,500, Color.web("#666970"));
        stage.setScene(scene);
        show();
        //scene.getStylesheets().add("webviewsample/BrowserToolbar.css");

    }

    public void show() {
        Platform.runLater(() -> {
            if (stage == null) {
                stage = new Stage();
                start(stage); // You might need to adjust your start() method
            }

            if (!stage.isShowing()) {
                stage.show();
            }
        });
    }

    class Browser extends Region {
        final WebView browser = new WebView();
        final WebEngine webEngine = browser.getEngine();

        public Browser() {
            webEngine.loadContent("<html><body>Dette er en test</body></html>");

            getChildren().add(browser);
        }


        @Override protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            layoutInArea(browser,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
        }

        @Override protected double computePrefWidth(double height) {
            return 750;
        }

        @Override protected double computePrefHeight(double width) {
            return 500;
        }
    }
}
