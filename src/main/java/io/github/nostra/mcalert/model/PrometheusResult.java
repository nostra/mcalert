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
        PrometheusData data,
        String name
) {
    /**
     * @return Debug string
     */
    public String debugOutput() {
        return "Number of alerts read["+name+"]: "+data.alerts().size();
    }

    public boolean noAlerts() {
        return data() != null && data.alerts().isEmpty();
    }

    public PrometheusResult addName( String name ) {
        return new PrometheusResult(status, data, name);
    }
}
