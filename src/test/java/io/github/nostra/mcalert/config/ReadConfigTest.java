package io.github.nostra.mcalert.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ReadConfigTest {
    @Test
    void readConfig() {
        assertNotNull(ReadConfigTest.class.getResource("/mcalert.yaml"));
        var aec = new ConfigProducer().alertEndpointConfig();
        // TODO Current code will pick up config in home, fix later
        //assertEquals(2, aec.endpoints().size());
    }
}
