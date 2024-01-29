package io.github.nostra.mcalert;

import io.github.nostra.mcalert.exception.McException;
import io.github.nostra.mcalert.tray.TrayCreator;
import jakarta.enterprise.context.Dependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class McService {
    private final Logger log = LoggerFactory.getLogger(McService.class);
    void execute() {
        log.info("Executing McService");
        TrayCreator.boot();
    }

    public void loadConfig(String config) {
        throw new McException("Not implemented yet, won't load "+config);
    }
}
