package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.model.AlertType;
import io.github.nostra.mcalert.model.FiringAlertMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

public class AlertMenuItem extends MenuItem implements PropertyChangeListener {
    private static final Logger log = LoggerFactory.getLogger(AlertMenuItem.class);
    public static final String EMOJI_BOMB = "\uD83D\uDCA3";
    public static final String EMOJI_CHECKMARK = "\u2714\uFE0F";
    public static final String EMOJI_GREEN_CIRCLE = "\uD83D\uDFE2";
    public static final String EMOJI_RED_CIRCLE = "\uD83D\uDD34";
    public static final String EMOJI_SOON = "\uD83D\uDD1C";
    public static final String EMOJI_DEACTIVATE = "\u23F9";

    private final String key;

    ///  @param endpointKey Name of endpoint, e.g. prometheus, grafana, etc
    public AlertMenuItem(String endpointKey) {
        super(EMOJI_SOON + " " + endpointKey);
        this.key = endpointKey;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!"firingAlerts".equals(evt.getPropertyName())) {
            return;
        }

        FiringAlertMeta[] alerts = (FiringAlertMeta[]) evt.getNewValue();

        FiringAlertMeta[] keyAlerts = Arrays.stream(alerts == null ? new FiringAlertMeta[0] : alerts)
                .filter(a -> key.equals(a.resourceKey()))
                .toArray(FiringAlertMeta[]::new);
        if ( alerts != null && keyAlerts.length != alerts.length ) { // TODO Remove later
            log.error("Good thing I check where the alert comes from");
        }

        // Check if we have exactly one alert and it's DEACTIVATED
        if (keyAlerts.length == 1 && keyAlerts[0].alertType() == AlertType.DEACTIVATED) {
            setLabel(EMOJI_DEACTIVATE + " " + key);
            return;
        }

        // Check if we have any active alerts
        boolean hasActiveAlerts = Arrays.stream(keyAlerts)
                .anyMatch(alert -> alert.alertType() == AlertType.ACTIVE);

        if (hasActiveAlerts) {
            setLabel(EMOJI_RED_CIRCLE + " " + key);
        } else {
            setLabel(EMOJI_GREEN_CIRCLE + " " + key);
        }
    }
}
