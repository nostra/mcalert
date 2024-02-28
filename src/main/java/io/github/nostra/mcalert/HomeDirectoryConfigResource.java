package io.github.nostra.mcalert;

import io.quarkus.runtime.annotations.StaticInitSafe;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@StaticInitSafe
public class HomeDirectoryConfigResource implements ConfigSource {
    private static final Logger logger = LoggerFactory.getLogger(HomeDirectoryConfigResource.class);

    private static final Map<String, String> configuration = new HashMap<>();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        String propertiesPath = null;
        try {
            Properties properties = new Properties();
            // file location is assumed to be in user's home directory here
            propertiesPath = System.getProperty("user.home") + "/.mcalert.properties";
            try (var input = Files.newInputStream(Path.of(propertiesPath))) {
                properties.load(input);
            }
            properties.forEach((key, value) -> configuration.put("" + key, "" + value));
        } catch (IOException e) {
            logger.info("Could not find a configuration file name ", propertiesPath);
        }
    }

    /**
     * Number comes from tutorial:
     * https://quarkus.io/guides/config-extending-support#example
     */
    @Override
    public int getOrdinal() {
        return 275;
    }

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return configuration.get(propertyName);
    }

    @Override
    public String getName() {
        return HomeDirectoryConfigResource.class.getSimpleName();
    }
}
