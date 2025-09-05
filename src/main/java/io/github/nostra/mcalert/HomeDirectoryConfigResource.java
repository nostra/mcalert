package io.github.nostra.mcalert;

import io.github.nostra.mcalert.exception.McConfigurationException;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.PropertiesConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.OptionalInt;

/// See https://quarkus.io/guides/config-extending-support#example
public class HomeDirectoryConfigResource implements ConfigSourceFactory {
    private static final Logger logger = LoggerFactory.getLogger(HomeDirectoryConfigResource.class);

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext) {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File configFile = new File(userHome, ".mcalert.properties");
            if (configFile.exists()) {
                try {
                    return Collections.singletonList(new PropertiesConfigSource(configFile.toURI().toURL()));
                } catch (IOException e) {
                    throw new McConfigurationException("Unexepected", e);
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(290);
    }
  }
