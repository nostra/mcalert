package io.github.nostra.mcalert.tray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;

class AlertMenuItemUnitTest {
    @Test
    void testPropertyChangeWithNegative666() {
        AlertMenuItem item = new AlertMenuItem("testKey");      
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "property", null, "-666");
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_DEACTIVATE + " " + "testKey", item.getLabel());
    }
    
    @Test
    void testPropertyChangeWithZero() {
        AlertMenuItem item = new AlertMenuItem("testKey");
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "property", null, "0");
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_GREEN_CIRCLE + " " + "testKey", item.getLabel());
    }
    
    @Test
    void testPropertyChangeWithNonZero() {
        AlertMenuItem item = new AlertMenuItem("testKey");
        PropertyChangeEvent evt = new PropertyChangeEvent(item, "property", null, "1");
        item.propertyChange(evt);
        assertEquals(AlertMenuItem.EMOJI_RED_CIRCLE + " " + "testKey", item.getLabel());
    }
}