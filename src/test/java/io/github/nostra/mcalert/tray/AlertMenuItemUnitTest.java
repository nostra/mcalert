package io.github.nostra.mcalert.tray;

import java.beans.PropertyChangeEvent;
import java.time.Instant;

import io.github.nostra.mcalert.model.AlertType;
import io.github.nostra.mcalert.model.FiringAlertMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true",
        disabledReason = "Github is headless, and cannot build AWT class")
class AlertMenuItemUnitTest {
    @Test
    void testEmptyAlerts() {
        AlertMenuItem item = new AlertMenuItem("testKey");
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "firingAlerts", null, new FiringAlertMeta[0]);
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_GREEN_CIRCLE + " " + "testKey", item.getLabel());
    }

    @Test
    void testSingleDeactivatedAlert() {
        AlertMenuItem item = new AlertMenuItem("testKey");
        FiringAlertMeta[] alerts = {new FiringAlertMeta("testKey", "SomeAleryKey", 0, Instant.now(), AlertType.DEACTIVATED)};
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "firingAlerts", null, alerts);
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_DEACTIVATE + " " + "testKey", item.getLabel());
    }

    @Test
    void testMultipleAlertsWithOneDeactivated() {
        AlertMenuItem item = new AlertMenuItem("testKey");
        FiringAlertMeta[] alerts = {
            new FiringAlertMeta("testKey", "SomeAlertValueType", 0, Instant.now(), AlertType.DEACTIVATED),
            new FiringAlertMeta("testKey", "SomeAlertValueType", 0, Instant.now(), AlertType.INACTIVE)
        };
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "firingAlerts", null, alerts);
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_GREEN_CIRCLE + " " + "testKey", item.getLabel());
    }

    @Test
    void testMultipleAlertsWithOneActive() {
        AlertMenuItem item = new AlertMenuItem("testKey");
        FiringAlertMeta[] alerts = {
            new FiringAlertMeta("testKey", "SomeAlertValueType", 1, Instant.now(), AlertType.ACTIVE),
            new FiringAlertMeta("testKey", "SomeAlertValueType", 0, Instant.now(), AlertType.INACTIVE)
        };
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "firingAlerts", null, alerts);
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_RED_CIRCLE + " " + "testKey", item.getLabel());
    }

    @Test
    void testOnlyInactiveAlerts() {
        AlertMenuItem item = new AlertMenuItem("testKey");
        FiringAlertMeta[] alerts = {
            new FiringAlertMeta("testKey", "SomeAlertValueType", 0, Instant.now(), AlertType.INACTIVE),
            new FiringAlertMeta("testKey", "SomeAlertValueType", 0, Instant.now(), AlertType.INACTIVE)
        };
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "firingAlerts", null, alerts);
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_GREEN_CIRCLE + " " + "testKey", item.getLabel());
    }

    @Test
    void testAlertsForDifferentKeys() {
        AlertMenuItem item = new AlertMenuItem("testKey");
        FiringAlertMeta[] alerts = {
            new FiringAlertMeta("differentKey", "SomeAlertValueType", 1, Instant.now(), AlertType.ACTIVE),
            new FiringAlertMeta("testKey", "SomeAlertValueType", 0, Instant.now(), AlertType.INACTIVE)
        };
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "firingAlerts", null, alerts);
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_GREEN_CIRCLE + " " + "testKey", item.getLabel());
    }
}
