package io.github.nostra.mcalert;

import io.github.nostra.mcalert.client.AlertResource;
import io.github.nostra.mcalert.client.SingleEndpointPoller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
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
        TabPane tabPane = new TabPane();

        alertResource.map().forEach((key, singleEndpointPoller) -> {
            final Tab tab = new Tab(key);
            tab.setClosable(false);
            ObservableList<Item> items = FXCollections.observableArrayList();
            singleEndpointPoller.firingAlerts().forEach(firing -> {
                boolean isIgnored = singleEndpointPoller.ignoredAlerts().contains(firing.name());
                items.add(new Item(firing.name(), isIgnored));
            });

            ListView<Item> listView = createListView(singleEndpointPoller, items);
            VBox vbox = new VBox();
            vbox.getChildren().add(new Label("Checked alerts are ignored:"));
            vbox.getChildren().add(listView);
            tab.setContent(vbox);
            tabPane.getTabs().add(tab);
        });

        Scene scene = new Scene(tabPane, 300, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    private ListView<Item> createListView(SingleEndpointPoller sep, ObservableList<Item> items) {
        ListView<Item> listView = new ListView<>(items);
        listView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Item> call(ListView<Item> param) {
                return new ListCell<>() {
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
                                // Only allowing toggle on non-watchdog alerts
                                if ( sep.toggleIgnoreOn( item.name )) {
                                    item.setSelected(newVal);
                                }
                            });
                            setGraphic(checkBox);
                        }
                    }
                };
            }
        });
        return listView;
    }
}