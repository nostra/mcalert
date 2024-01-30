package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.PrometheusResult;
import org.eclipse.microprofile.rest.client.inject.RestClient;

//@Path("/v1")
public class AlertResource {
    @RestClient
    AlertService alertService;

    //@GET
    //@Path("/alerts")
    public PrometheusResult get() {
        return alertService.getResult();
    }
}
