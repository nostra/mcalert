package io.github.nostra.mcalert;

import io.github.nostra.mcalert.client.SingleEndpointPoller;
import io.github.nostra.mcalert.model.Item;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class MaclertTab extends Tab {
    private static final Logger logger = LoggerFactory.getLogger(MaclertTab.class);

    public MaclertTab(String endpointName) {
        super(endpointName);
    }

    protected ObservableList<Item> createItemsFromAlerts(SingleEndpointPoller poller) {
        ObservableList<Item> items = FXCollections.observableArrayList();

        poller.firingAlerts().forEach(alert -> {
            boolean isIgnored = poller.ignoredAlerts().contains(alert.name());

            // Calculate how long since the alert was last seen
            long seenSecondsAgo = (alert.lastSeen() == null)
                    ? 50_000  // Default high value if never seen
                    : Instant.now().getEpochSecond() - alert.lastSeen().getEpochSecond();

            items.add(new Item(alert.name(), isIgnored, seenSecondsAgo));
        });

        return items;
    }

    protected ListView<Item> createListView(SingleEndpointPoller sep, ObservableList<Item> items) {
        ListView<Item> listView = new ListView<>(items);
        listView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    updateNonEmptyItem(item);
                }
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
                if (sep.isWatchDogAlert(item.getName())) {
                    checkBox.setDisable(true);
                }
                return checkBox;
            }

            private void setupCheckBoxListener(CheckBox checkBox, Item item) {
                checkBox.selectedProperty().addListener((_, _, newVal) -> {
                    item.setSeenSecondsAgo(0);
                    if (sep.toggleIgnoreOn(item.getName())) {
                        item.setSelected(newVal);
                    }
                });
            }

            private void applyAgeBasedStyle(CheckBox checkBox, Item item) {
                if (item.getSeenSecondsAgo() > 40_000) {
                    checkBox.setStyle("""
                            -fx-background-color: rgb(255, 204, 203);
                            -fx-strikethrough: true;
                            """.stripIndent());
                } else if (item.getSeenSecondsAgo() > 10) {
                    // TODO If this is a missing watchdog alert, the color is misleading
                    int maxSeconds = 2000;
                    int seenSecondsAgo = Math.min((int) item.getSeenSecondsAgo(), maxSeconds);
                    int greenIntensity = Math.max(255 - (seenSecondsAgo * 255 / maxSeconds), 125);
                    checkBox.setStyle(String.format("-fx-background-color: rgb(0, %d, 0);", greenIntensity));
                } else {
                    checkBox.setStyle("-fx-background-color: white;");
                }
            }
        });
        return listView;
    }

    /// Initialization method
    public MaclertTab withEndpointPoller(SingleEndpointPoller poller) {
        updateContentsOfTab(poller);
        poller.setTab(this);
        return this;
    }

    /// Read list of alerts from poller, and update tab contents
    /// with updated list view elements
    public void updateContentsOfTab(SingleEndpointPoller poller) {
        // Convert firing alerts to observable items
        ObservableList<Item> items = createItemsFromAlerts(poller);

        // Create and configure the list view with the items
        ListView<Item> listView = createListView(poller, items.sorted());
        Platform.runLater(() -> setContent(listView));
    }

    public void updateMarker(boolean success) {
        int space = getText().indexOf(" ");
        if ( success && space > 0 ) {
            Platform.runLater(() -> setText(getText().substring(space+1)));
        }
        if ( !success && space < 0 ) {
            // Alternatives would be
            //int emojiCode = 0x1F4A2; // Unicode for anger symbol
            //String emoji = new String(Character.toChars(emojiCode));
            Platform.runLater(() ->setText("* "+getText()));
        }
    }
}
