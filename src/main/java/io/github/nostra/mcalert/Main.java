package io.github.nostra.mcalert;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Semaphore;

@QuarkusMain
@CommandLine.Command
public class Main implements QuarkusApplication, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final McService mcService;

    @CommandLine.Option(names = {"-c", "--config"}, description = "Configuration")
    String config;

    public Main(McService mcService) {
        this.mcService = mcService;
    }

    @Override
    public int run(String... args) {
        return new CommandLine(new Main(mcService)).execute(args);
    }

    @Override
    public void run() {
        if (!java.awt.SystemTray.isSupported()) {
            logger.error("No system tray support, application exiting.");
            Quarkus.asyncExit(2);
            return;
        }

        if (config != null) {
            mcService.loadConfig(config);
        }
        Semaphore mutex =  mcService.execute();
        try {
            logger.info("Execute done, now block for exit");
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            logger.info("Exiting");
            mutex.release();
        }
    }
}
