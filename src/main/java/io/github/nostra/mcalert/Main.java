package io.github.nostra.mcalert;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

@QuarkusMain
@CommandLine.Command
public class Main implements QuarkusApplication {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final McTrayService mcTrayService;

    // Keeping the variable to get a nice cli explanation
    @CommandLine.Option(names = "--no-tray", description = "Disable system tray support")
    public boolean noTray;

    public Main(McTrayService mcTrayService) {
        this.mcTrayService = mcTrayService;
    }

    @Override
    public int run(String... args) {
        logger.error("Got args " + List.of(args));
        logger.error("Value of noTray: " + noTray);
        // For some reason, the "noTray" variable is not correctly populated
        boolean reallyNoTray = noTray
                || Set.of(args).contains("--no-tray")
                || System.getProperty("NO_TRAY") != null
                || !java.awt.SystemTray.isSupported();
        new Thread(() -> StatusWindow.doIt()).start();
        Semaphore mutex;
        if ( reallyNoTray) {
            logger.info("System tray support disabled by --no-tray argument or system variable.");
            logger.warn("Currently exiting, later do something useful");
            mutex = mcTrayService.startWithoutTray();
        } else {
            logger.info("System tray support enabled.");
            mutex = mcTrayService.startTray();
        }

        logger.info("Execute done, now block for exit");
        mutex.acquireUninterruptibly();
        logger.info("Exiting");
        mutex.release();

        return 0;
    }
}
