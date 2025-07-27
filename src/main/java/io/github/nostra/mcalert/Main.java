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

    private final McTrayService mcTrayService;

    public Main(McTrayService mcTrayService) {
        this.mcTrayService = mcTrayService;
    }

    @Override
    public int run(String... args) {
        return new CommandLine(new Main(mcTrayService)).execute(args);
    }

    @Override
    public void run() {
        if (!java.awt.SystemTray.isSupported()) {
            logger.error("No system tray support, application exiting.");
            Quarkus.asyncExit(2);
            return;
        }

        new Thread(() -> StatusWindow.doIt()).start();
        Semaphore mutex =  mcTrayService.execute();
        try {
            logger.info("Execute done, now block for exit");
            mutex.acquire();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } finally {
            logger.info("Exiting");
            mutex.release();
        }
    }
}
