package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/extensions")
@RegisterRestClient
public interface AlertService {
    @GET
    @Path("/v1/alerts")
    PrometheusResult getResult();

}
