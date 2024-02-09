package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "single-client")
public interface AlertService {
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    PrometheusResult getResult();

}
