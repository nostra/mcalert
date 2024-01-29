package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/v1/alerts")
public class AlertResource {
    @RestClient
    AlertService alertService;

    @GET
    @Path("/")
    public PrometheusResult get() {
        return alertService.getResult();
    }
}
