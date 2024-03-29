package io.github.nostra.mcalert.tray;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AlertMenuItem extends MenuItem implements PropertyChangeListener {
    public static final String EMOJI_BOMB = "\uD83D\uDCA3";
    public static final String EMOJI_CHECKMARK = "\u2714\uFE0F";
    public static final String EMOJI_GREEN_CIRCLE = "\uD83D\uDFE2";
    public static final String EMOJI_RED_CIRCLE = "\uD83D\uDD34";
    public static final String EMOJI_SOON = "\uD83D\uDD1C";
    public static final String EMOJI_DEACTIVATE = "\u23F9";

    private final String key;

    public AlertMenuItem(String key) {
        super(EMOJI_SOON + " " + key);
        this.key = key;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        int value = Integer.parseInt("" + evt.getNewValue());
        
        if (-666 == value ) {
            setLabel(EMOJI_DEACTIVATE + " " + key);
        } else if (0 != value ) {
            setLabel(EMOJI_RED_CIRCLE + " " + key);
        } else {
            setLabel(EMOJI_GREEN_CIRCLE + " " + key);
        }
    }
}
