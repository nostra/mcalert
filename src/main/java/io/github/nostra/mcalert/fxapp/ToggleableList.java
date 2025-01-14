package io.github.nostra.mcalert.fxapp;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ToggleableList extends Application {

    public static class Item {
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
    }

    @Override
    public void start(Stage primaryStage) {
        ObservableList<Item> items = FXCollections.observableArrayList(
                new Item("Item 1", false),
                new Item("Item 2", true),
                new Item("Item 3", false)
        );

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
                            });
                            setGraphic(checkBox);
                        }
                    }
                };
            }
        });

        VBox root = new VBox(listView);
        Scene scene = new Scene(root, 300, 250);

        primaryStage.setTitle("Toggleable List");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}