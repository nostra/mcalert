package io.github.nostra.mcalert.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nostra.mcalert.exception.McException;
import io.github.nostra.mcalert.model.PrometheusResult;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

class AlertResourceTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSerialization() {
        PrometheusResult result = readPrometheusData();
        assertNotNull(result);
        assertEquals("success", result.status());
        assertEquals(11, result.data().alerts().size());

        assertEquals("firing", result.data().alerts().getFirst().state());
        assertEquals("2024-01-30T07:06:09.236847853Z", result.data().alerts().getFirst().activeAt());
    }

    @Test
    void testFilteredResult() {
        AlertResource alertResource = new AlertResource();
        alertResource.namesToIgnore = List.of("CPUThrottlingHigh");
        alertResource.alertService = this::readPrometheusData;
        var filtered = alertResource.getFiltered();
        assertFalse(
                filtered.data()
                        .alerts()
                        .stream()
                        .anyMatch(alert -> alert.alertName().equals("CPUThrottlingHigh")));

        assertEquals(4, filtered.data().alerts().size());
        assertFalse(filtered.noAlerts());
    }

    private PrometheusResult readPrometheusData() {
        try {
            URI uri = getClass().getClassLoader().getResource("prometheus-data1.json").toURI();
            String content = Files.readString(Paths.get(uri), StandardCharsets.UTF_8);
            return objectMapper.readValue(content, PrometheusResult.class);
        } catch (Exception e) {
            throw new McException("Trouble reading prometheus test file", e);
        }
    }
}