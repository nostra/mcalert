package io.github.nostra.mcalert;

import io.github.nostra.mcalert.client.AlertResource;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
    public static class Item implements Comparable<Item> {
        private String name;
        private boolean selected;

        public Item(String name, boolean selected) {
            this.name = name;
            this.selected = selected;
        }

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public int compareTo(Item other) {
            return name.compareTo(other.name);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("STarting status window");
        instance = this;
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(false);
        primaryStage.setTitle("Toggleable List");
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
        alertResource.map().keySet().stream().sorted().forEach(key -> logger.info("Need to create tab for "+key));

        ObservableList<Item> items = FXCollections.observableArrayList();
        alertResource.map()
                .keySet()
                .stream()
                .filter(key -> alertResource.map().get(key).isActive()) // TODO want separate lists
                // The map entry below is just to create a tuple
                .map(key -> Map.entry( alertResource.map().get(key).ignoredAlerts(), alertResource.map().get(key).firingAlerts()))
                .forEach( entry -> {
                    var ignored = entry.getKey();
                    entry.getValue()
                            .stream()
                            .map(firing -> new Item(firing.name(), ignored.contains(firing.name())))
                            .sorted()
                            .forEach(items::add);
                });

        Scene scene = createScene(items);

        primaryStage.setScene(scene);


        // Show the stage (window)
        primaryStage.show();
    }

    private static Scene createScene(ObservableList<Item> items) {
        ListView<Item> listView = new ListView<>(items);
        listView.setCellFactory(new Callback<ListView<Item>, ListCell<Item>>() {
            @Override
            public ListCell<Item> call(ListView<Item> param) {
                return new ListCell<Item>() {
                    @Override
                    protected void updateItem(Item item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            CheckBox checkBox = new CheckBox(item.getName());
                            checkBox.setSelected(item.isSelected());
                            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                                item.setSelected(newVal);
                                logger.info("Toggle called on "+item.name);
                            });
                            setGraphic(checkBox);
                        }
                    }
                };
            }
        });

        VBox root = new VBox(listView);
        Scene scene = new Scene(root, 300, 250);
        return scene;
    }

}
