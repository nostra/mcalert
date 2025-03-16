package io.github.nostra.mcalert;

import io.github.nostra.mcalert.client.AlertResource;
import io.github.nostra.mcalert.client.SingleEndpointPoller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusWindow extends Application {
    private static final Logger logger = LoggerFactory.getLogger(StatusWindow.class);
    private static StatusWindow instance;
    private Stage primaryStage;

    public static void doIt() {
        launch();
    }

    public static StatusWindow getInstance() {
        return instance;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting status window");
        instance = this;
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(false);
        primaryStage.setTitle("Firing alerts");
    }

    public void show(AlertResource alertResource) {
        Platform.runLater(() -> {
            if (primaryStage.isShowing()) {
                logger.info("Bring to front");
                primaryStage.toFront(); // Bring to front if already visible
            } else {
                logger.info("Create and show");
                createAndShowGUI(alertResource);
            }
        });
    }

    private void createAndShowGUI(AlertResource alertResource) {
        TabPane tabPane = createTabPane();

        alertResource.map().forEach((endpointName, poller) -> {
            Tab tab = createEndpointTab(endpointName, poller);
            tabPane.getTabs().add(tab);
        });

        Scene scene = new Scene(tabPane, 300, 250);

        // Bind TabPane dimensions to Scene for proper resizing
        tabPane.prefWidthProperty().bind(scene.widthProperty());
        tabPane.prefHeightProperty().bind(scene.heightProperty());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        // Configure TabPane to fill available space
        tabPane.setPrefWidth(Double.MAX_VALUE);
        tabPane.setPrefHeight(Double.MAX_VALUE);
        tabPane.setMaxWidth(Double.MAX_VALUE);
        tabPane.setMaxHeight(Double.MAX_VALUE);
        return tabPane;
    }

    private Tab createEndpointTab(String endpointName, SingleEndpointPoller poller) {
        final MaclertTab tab = new MaclertTab(endpointName)
                .withEndpointPoller( poller );
        tab.setClosable(false);

        return tab;
    }
}
