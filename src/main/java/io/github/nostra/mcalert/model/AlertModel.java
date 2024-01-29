package io.github.nostra.mcalert.model;

import java.util.Map;

/**
 *       {
 *         "labels": {
 *           "alertname": "KubeSchedulerDown",
 *           "severity": "critical"
 *         },
 *         "annotations": {
 *           "description": "KubeScheduler has disappeared from Prometheus target discovery.",
 *           "runbook_url": "https://runbooks.prometheus-operator.dev/runbooks/kubernetes/kubeschedulerdown",
 *           "summary": "Target disappeared from Prometheus target discovery."
 *         },
 *         "state": "firing",
 *         "activeAt": "2024-01-26T07:16:27.844106554Z",
 *         "value": "1e+00"
 *       },
 */
public class AlertModel {
    public Map<String, String> labels;
    public Map<String, String> annotations;
    public String state;
    public String activeAt;
    public double value;
}
