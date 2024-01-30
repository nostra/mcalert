package io.github.nostra.mcalert.model;

/**
 * {
 *   "status": "success",
 *   "data": {
 *     "alerts": [
 *     ...
 */
public class PrometheusResult {
    public String status;
    public PrometheusData data;
}
