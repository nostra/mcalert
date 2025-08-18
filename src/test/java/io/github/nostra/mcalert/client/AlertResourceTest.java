package io.github.nostra.mcalert.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nostra.mcalert.config.AlertEndpointConfig;
import io.github.nostra.mcalert.exception.McException;
import io.github.nostra.mcalert.model.GrafanaDatasource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class AlertResourceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void testPrometheusSerialization() {
        PrometheusResult result = readPrometheusData();

        assertNotNull(result);
        assertEquals("success", result.status());
        assertEquals(11, result.data().alerts().size());

        assertEquals("firing", result.data().alerts().getFirst().state());
        assertEquals("2024-01-30T07:06:09.236847853Z", result.data().alerts().getFirst().activeAt());
    }

    @Test
    void testGrafanaSerialization() {
        List<GrafanaDatasource> result = readGrafanaData();

        assertNotNull(result);
        assertEquals(1, result.size());
        var item = result.getFirst();
        assertEquals("prometheus", item.name());
        assertEquals("P1809F7CD0C75ACF3", item.uid());
    }

    @Test
    void testFilteredResult() {
        SingleEndpointPoller sep = createMockPoller(List.of("CPUThrottlingHigh"), new ArrayList<>());
        var filtered = sep.callPrometheus("junit");
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
        SingleEndpointPoller sep = createMockPoller(
                List.of("CPUThrottlingHigh", "NodeClockNotSynchronising", "KubeControllerManagerDown", "KubeSchedulerDown"),
                new ArrayList<>()
        );
        var filtered = sep.callPrometheus("junit");

        assertTrue(filtered.noAlerts(), "When masking out all elements, no alerts should be left");
    }

    @Test
    void testThatWatchdogGetsFound() {
        SingleEndpointPoller sep = createMockPoller(
                List.of("NodeClockNotSynchronising", "KubeControllerManagerDown", "KubeSchedulerDown"),
                List.of("CPUThrottlingHigh"));
        assertTrue(sep.callPrometheus("junit").noAlerts(), "Watchdog makes all become masked");
    }

    @Test
    void testThatMissingWatchdogGivesError() {
        SingleEndpointPoller sep = createMockPoller(
                List.of("CPUThrottlingHigh", "NodeClockNotSynchronising", "KubeControllerManagerDown", "KubeSchedulerDown"),
                List.of("NonExistingName"));
        var filtered = sep.callPrometheus("junit");

        assertFalse(filtered.noAlerts());
        assertEquals(1, filtered.data().alerts().size());
    }

    private SingleEndpointPoller createMockPoller(final List<String> ignore, final List<String> watchdog) {
        AlertEndpointConfig.AlertEndpoint cfg = new AlertEndpointConfig.AlertEndpoint() {
            @Override
            public URI uri() {
                return null;
            }

            @Override
            public Optional<String> datasource() {
                return Optional.empty();
            }

            @Override
            public Optional<List<AlertEndpointConfig.Header>> header() {
                return Optional.empty();
            }

            @Override
            public List<String> ignoreAlerts() {
                return ignore;
            }

            @Override
            public List<String> watchdogAlerts() {
                return watchdog;
            }
        };
        AlertCaller junitMockCaller = new AlertCaller() {
            @Override
            public PrometheusResult callPrometheus() {
                return readPrometheusData();
            }

            @Override
            public List<GrafanaDatasource> callGrafana() {
                return List.of();
            }
        };
        return new SingleEndpointPoller("junit", cfg, junitMockCaller);
    }

    private List<GrafanaDatasource> readGrafanaData() {
        try {
            URI uri = getClass().getClassLoader().getResource("grafana-datasource.json").toURI();
            String content = Files.readString(Paths.get(uri), StandardCharsets.UTF_8);
            return objectMapper.readValue(content, new TypeReference<>() {});
        } catch (Exception e) {
            throw new McException("Trouble reading prometheus test file", e);
        }
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