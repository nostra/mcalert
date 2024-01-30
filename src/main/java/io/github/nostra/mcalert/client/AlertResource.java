package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.AlertModel;
import io.github.nostra.mcalert.model.PrometheusResult;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

public class AlertResource {
    @RestClient
    AlertService alertService;

    @ConfigProperty(name = "prometheus.ignore.alerts", defaultValue = "")
    List<String> namesToIgnore;

    public PrometheusResult getFiltered() {
        PrometheusResult result = alertService.getResult();
        List<AlertModel> toRemove = result
                .data()
                .alerts()
                .stream()
                .filter(alertModel -> namesToIgnore.contains(alertModel.alertName()))
                .toList();
        result.data().alerts().removeAll(toRemove);
        return result;
    }
}
