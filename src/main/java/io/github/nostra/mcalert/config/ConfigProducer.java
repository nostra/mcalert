package io.github.nostra.mcalert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.github.nostra.mcalert.exception.McConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@ApplicationScoped
public class ConfigProducer {
    private static final Logger logger = LoggerFactory.getLogger(ConfigProducer.class);

    @Produces
    @ApplicationScoped
    AlertEndpointConfig alertEndpointConfig() {
        failIfOldConfigPresent();
        File cfgFile = new File("mcalert.yaml");
        if (!cfgFile.isFile()) {
            cfgFile = new File(System.getProperty("user.home"), "mcalert.yaml");
        }
        try {
            InputStream resource;
            if (cfgFile.isFile()) {
                logger.info("Reading configuration from {} ", cfgFile.toPath());
                resource = Files.newInputStream(cfgFile.toPath());
            } else {
                logger.info("Could not find any mcalert.yaml files, using internal demo config. Please configure with a mcalert.yaml file");
                resource = this.getClass().getResourceAsStream("/mcalert.yaml");
            }

            ObjectMapper om = new ObjectMapper(new YAMLFactory())
                .registerModule(new Jdk8Module());

            return om.readValue(resource, AEConfig.class);
        } catch (IOException e) {
            throw new McConfigurationException("Unexpected problem reading resource", e);
        }
    }

    private void failIfOldConfigPresent() {
        File oldCfg = new File(System.getProperty("user.home"), ".mcalert.properties");
        if (oldCfg.exists()) {
            throw new McConfigurationException("Found " + oldCfg.getAbsolutePath() + ". Please remove it and configure mcalert.yaml");
        }
        logger.info("\n\n\nOLD CONFIG DOES NOT EXIST - {} ", oldCfg.getAbsolutePath());
    }
}
