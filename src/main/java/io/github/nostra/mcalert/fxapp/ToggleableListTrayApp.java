package io.github.nostra.mcalert.fxapp;

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

import java.awt.*;

public class ToggleableListTrayApp extends Application {

    private Stage primaryStage; // Keep a reference to the stage

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
        this.primaryStage = primaryStage; // Store the stage for later use

        // Ensure that Platform.exit() is not called when the window is closed
        Platform.setImplicitExit(false);

        createTrayIcon(); // Set up the tray icon first

        primaryStage.setTitle("Toggleable List");

        // Initially hide the stage instead of showing it
        // primaryStage.show();
    }

    private void createAndShowGUI() {
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

        // Set the scene only if it hasn't been set before to avoid exceptions
        if (primaryStage.getScene() == null) {
            primaryStage.setScene(scene);
        }

        // Show the stage (window)
        primaryStage.show();
    }

    private void createTrayIcon() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }

        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(createImage("/images/pulse-line.png", "tray icon")); // Replace icon.png
        final SystemTray tray = SystemTray.getSystemTray();

        // Add components to the popup menu
        MenuItem showItem = new MenuItem("Show Window");
        MenuItem exitItem = new MenuItem("Exit");

        popup.add(showItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
            return;
        }

        // Show the window when "Show Window" is clicked
        showItem.addActionListener(e -> {
            Platform.runLater(() -> {
                if (primaryStage.isShowing()) {
                    primaryStage.toFront(); // Bring to front if already visible
                } else {
                    createAndShowGUI();
                }
            });
        });

        // Exit the application when "Exit" is clicked
        exitItem.addActionListener(e -> {
            Platform.exit(); // Shutdown JavaFX
            tray.remove(trayIcon); // Remove the tray icon
            System.exit(0); // Terminate the application
        });
    }

    // Helper method to create an Image for the tray icon
    protected static Image createImage(String path, String description) {
        // Load the image (you might need to adjust the path)
        return Toolkit.getDefaultToolkit().getImage(ToggleableListTrayApp.class.getResource(path));
    }

    public static void main(String[] args) {
        launch(args);
    }
}