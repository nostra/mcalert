package io.github.nostra.mcalert.model;

import java.util.Map;

/// Example value
/// ```
///{
///         "labels": {
///           "alertname": "KubeSchedulerDown",
///           "severity": "critical"
///},
///         "annotations": {
///           "description": "KubeScheduler has disappeared from Prometheus target discovery.",
///           "runbook_url": "https://runbooks.prometheus-operator.dev/runbooks/kubernetes/kubeschedulerdown",
///           "summary": "Target disappeared from Prometheus target discovery."
///},
///         "state": "firing",
///         "activeAt": "2024-01-26T07:16:27.844106554Z",
///         "value": "1e+00"
///},
///```
///
/// @param value Usually a double value, but sometimes it ends with +Inf, which fails during conversion. Therefore
///              it is kept as a string.
public record AlertModel(
        Map<String, String> labels,
        Map<String, String> annotations,
        String state,
        String activeAt,
        String value
) {
    public String alertName() {
        return labels.getOrDefault("alertname", "ALERT_NAME_NULL");
    }

    /// @return Try to find something interesting to return, or empty string of nothing found. Usually,
    /// the description annotation would be nice
    public String descriptionFieldFromAlert() {
        return annotations()
                .getOrDefault("description", annotations()
                        .getOrDefault("summary", ""));
    }

}
