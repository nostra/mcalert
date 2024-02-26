package io.github.nostra.mcalert.tray;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AlertMenuItem extends MenuItem implements PropertyChangeListener {
    public static final String EMOJI_BOMB = "\uD83D\uDCA3";
    public static final String EMOJI_CHECKMARK = "\u2714\uFE0F";

    private final String key;

    public AlertMenuItem(String key) {
        super("? "+ key);
        this.key = key;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ( Boolean.TRUE == evt.getNewValue()) {
            setLabel(EMOJI_BOMB + " "+key);
        } else {
            setLabel(EMOJI_CHECKMARK + " "+key);
        }
    }
}
