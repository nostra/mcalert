package io.github.nostra.mcalert.model;

/**
 * {
 *   "status": "success",
 *   "data": {
 *     "alerts": [
 *     ...
 */
public record PrometheusResult(
        String status,
        PrometheusData data
) {
    /**
     * @return Debug string
     */
    public String debugOutput() {
        return "Number of alerts read: "+data.alerts().size();
    }
}
