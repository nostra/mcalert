package io.github.nostra.mcalert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.io.IOException;

@ApplicationScoped
public class ConfigProducer {

    @Produces
    @ApplicationScoped
    AlertEndpointConfig alertEndpointConfig() {
        var resource = this.getClass().getResource("/mcalert.yaml");
        ObjectMapper om = new ObjectMapper(new YAMLFactory())
            .registerModule(new Jdk8Module());

        try {
            return om.readValue(resource, AEConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
