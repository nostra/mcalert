package io.github.nostra.mcalert.splash;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.time.Duration;
import java.time.Instant;

public final class SplashEventBus {
    private static final BooleanProperty closeRequested = new SimpleBooleanProperty(false);

    private SplashEventBus() {}

    private static final Duration MIN_SPLASH_TIME = Duration.ofSeconds(2);
    private static final Instant startTime = Instant.now();

    public static BooleanProperty closeRequestedProperty() {
        return closeRequested;
    }

    public static void requestClose() {
        if (Platform.isFxApplicationThread()) {
            closeRequested.set(true);
        } else {
            Runnable pauseAndClose = () -> {
                Duration remaining = MIN_SPLASH_TIME.minus(Duration.between(startTime, Instant.now()));
                if (!remaining.isNegative() && !remaining.isZero()) {
                    try {
                        Thread.sleep(remaining);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                Platform.runLater(() -> closeRequested.set(true));
            };
            Thread.startVirtualThread(pauseAndClose);
        }
    }
}
