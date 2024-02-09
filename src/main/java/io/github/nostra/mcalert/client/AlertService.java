package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.PrometheusResult;
import io.smallrye.config.ConfigMapping;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;

@ConfigMapping(prefix = "mcalert.prometheus")
public interface AlertService {
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    PrometheusResult callPrometheus();
}
