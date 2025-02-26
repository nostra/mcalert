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

import java.time.Instant;

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
        private long seenSecondsAgo;

        public Item(String name, boolean selected, long seenSecondsAgo) {
            this.name = name;
            this.selected = selected;
            this.seenSecondsAgo = seenSecondsAgo;
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

        public long getSeenSecondsAgo() {
            return seenSecondsAgo;
        }

        public void setSeenSecondsAgo(long seenSecondsAgo) {
            this.seenSecondsAgo = seenSecondsAgo;
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
                long seenSecondsAgo = firing.lastSeen() == null
                        ? 0
                        : Instant.now().getEpochSecond()-firing.lastSeen().getEpochSecond();
                items.add(new Item(firing.name(), isIgnored, seenSecondsAgo));
            });

            ListView<Item> listView = createListView(singleEndpointPoller, items.sorted());
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
                            clearCell();
                        } else {
                            updateNonEmptyItem(item);
                        }
                    }

                    private void clearCell() {
                        setText(null);
                        setGraphic(null);
                    }

                    private void updateNonEmptyItem(Item item) {
                        CheckBox checkBox = createCheckBox(item);
                        setupCheckBoxListener(checkBox, item);
                        setGraphic(checkBox);
                        applyAgeBasedStyle(checkBox, item);
                    }

                    private CheckBox createCheckBox(Item item) {
                        CheckBox checkBox = new CheckBox(item.getName());
                        checkBox.setSelected(item.isSelected());
                        return checkBox;
                    }

                    private void setupCheckBoxListener(CheckBox checkBox, Item item) {
                        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                            item.setSeenSecondsAgo(0);
                            if (sep.toggleIgnoreOn(item.name)) {
                                item.setSelected(newVal);
                            }
                        });
                    }

                    private void applyAgeBasedStyle(CheckBox checkBox, Item item) {
                        if (item.getSeenSecondsAgo() > 500) {
                            int maxSeconds = 5000;
                            int seenSecondsAgo = Math.min((int)item.getSeenSecondsAgo(), maxSeconds);
                            int greenIntensity = Math.max(255 - (seenSecondsAgo * 255 / maxSeconds), 75);
                            checkBox.setStyle(String.format("-fx-background-color: rgb(0, %d, 0);", greenIntensity));
                        } else {
                            checkBox.setStyle("-fx-background-color: white;");
                        }
                    }
                };
            }
        });
        return listView;
    }
}
