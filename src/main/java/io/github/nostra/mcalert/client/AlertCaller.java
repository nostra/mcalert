package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.GrafanaDatasource;
import io.github.nostra.mcalert.model.PrometheusResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

public interface AlertCaller {
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    PrometheusResult callPrometheus();

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    List<GrafanaDatasource> callGrafana();
}
