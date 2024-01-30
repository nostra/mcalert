package io.github.nostra.mcalert.model;

import java.util.List;

public record PrometheusData(
        List<AlertModel> alerts
) {
}
