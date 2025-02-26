package io.github.nostra.mcalert.model;

import java.time.Instant;

/// Retain meta data for a single alert
public record FiringAlertMeta(
        String resourceKey,
        String name,
        int numberOfAlerts,
        Instant lastSeen,
        AlertType alertType
        ) {
    public FiringAlertMeta increment() {
        return new FiringAlertMeta(resourceKey, name, numberOfAlerts + 1, Instant.now(), alertType);
    }
    // Consider info about disable
    /*
    DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                                            .withZone(ZoneId.systemDefault());
        String formattedLastSeen = formatter.format(user.getLastSeen());
     */
}
