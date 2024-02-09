package io.github.nostra.mcalert.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nostra.mcalert.exception.McException;
import io.github.nostra.mcalert.model.PrometheusResult;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

class AlertResourceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static PrometheusResult retrieveFirstFiringAndRelevantAlert(AlertResource alertResource) {
        return alertResource.getFiringAndRelevant().values().stream().iterator().next();
    }

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
        AlertResource alertResource = new AlertResource(null);
        alertResource.namesToIgnore = List.of("CPUThrottlingHigh");
        alertResource.watchdogAlertNames = List.of();
        alertResource.alertService = createNamedAlertService();
        var filtered = retrieveFirstFiringAndRelevantAlert(alertResource);
        assertFalse(
                filtered.data()
                        .alerts()
                        .stream()
                        .anyMatch(alert -> alert.alertName().equals("CPUThrottlingHigh")));

        assertEquals(4, filtered.data().alerts().size());
        assertFalse(filtered.noAlerts());
    }

    @Test
    void testAllThatFireAreIgnored() {
        AlertResource alertResource = new AlertResource(null);
        alertResource.namesToIgnore = List.of("CPUThrottlingHigh", "NodeClockNotSynchronising", "KubeControllerManagerDown", "KubeSchedulerDown");
        alertResource.watchdogAlertNames = List.of();
        alertResource.alertService = createNamedAlertService();
        assertTrue(retrieveFirstFiringAndRelevantAlert(alertResource).noAlerts(), "When masking out all elements, no alerts should be left");
    }

    @Test
    void testThatWatchdogGetsFound() {
        AlertResource alertResource = new AlertResource(null);
        alertResource.namesToIgnore = List.of("NodeClockNotSynchronising", "KubeControllerManagerDown", "KubeSchedulerDown");
        alertResource.watchdogAlertNames = List.of("CPUThrottlingHigh");
        alertResource.alertService = createNamedAlertService();
        assertTrue(retrieveFirstFiringAndRelevantAlert(alertResource).noAlerts(), "When masking out all elements, no alerts should be left");
    }

    @Test
    void testThatMissingWatchdogGivesException() {
        AlertResource alertResource = new AlertResource(null);
        alertResource.namesToIgnore = List.of("CPUThrottlingHigh", "NodeClockNotSynchronising", "KubeControllerManagerDown", "KubeSchedulerDown");
        alertResource.watchdogAlertNames = List.of("NonExistingName");
        alertResource.alertService = createNamedAlertService();
        assertFalse(retrieveFirstFiringAndRelevantAlert(alertResource).noAlerts());
        assertEquals(1, retrieveFirstFiringAndRelevantAlert(alertResource).data().alerts().size());
    }


    private Map<String, AlertService> createNamedAlertService() {
        return Map.of("junit", this::readPrometheusData);
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