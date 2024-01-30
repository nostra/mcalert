package io.github.nostra.mcalert.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.nostra.mcalert.model.PrometheusResult;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

class AlertResourceTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSerialization() throws IOException, URISyntaxException {
        URI uri = getClass().getClassLoader().getResource("prometheus-data1.json").toURI();
        String content = Files.readString(Paths.get(uri), StandardCharsets.UTF_8);
        assertNotNull( content );
        PrometheusResult result = objectMapper.readValue(content, PrometheusResult.class);
        assertNotNull(result);
        assertEquals("success", result.status);
        assertEquals(11, result.data.alerts.size());

        assertEquals("firing", result.data.alerts.get(0).state);
        assertEquals("2024-01-30T07:06:09.236847853Z", result.data.alerts.get(0).activeAt);
    }
}