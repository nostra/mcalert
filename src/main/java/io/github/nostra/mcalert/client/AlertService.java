package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v1")
@RegisterRestClient
public interface AlertService {
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/alerts")
    PrometheusResult getResult();

}
