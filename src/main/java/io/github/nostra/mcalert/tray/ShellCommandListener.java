package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.client.AlertResource;
import io.github.nostra.mcalert.client.EndpointCallEnum;
import io.github.nostra.mcalert.config.AlertEndpointConfig;
import io.github.nostra.mcalert.model.FiringAlertMeta;
import io.quarkus.runtime.Shutdown;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Dependent
public class ShellCommandListener implements PropertyChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(ShellCommandListener.class);
    private final AlertResource alertResource;
    private String shellCommand;
    private final AlertEndpointConfig config;

    @Inject
    public ShellCommandListener( AlertResource alertResource, AlertEndpointConfig config) {
        this.alertResource = alertResource;
        this.config = config;
    }

    @PostConstruct
    public void init() {
        if ( config.commandLine().isPresent() ) {
            shellCommand = config.commandLine().get().shellCommand();
            if ( !new File(shellCommand).canExecute() ){
                logger.warn("File to execute not found: {}", shellCommand);
                logger.warn("Please add the file and make it executable");
            }
            alertResource.addPropertyChangeListener(this);
            alertResource.map().values().forEach(singleEndpointPoller -> singleEndpointPoller.addPropertyChangeListener(this));
        }
    }

    @Shutdown
    void shutdown() {
        alertResource.removePropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ( "statusChange".equals(evt.getPropertyName())) {
            EndpointCallEnum collatedStatus = (EndpointCallEnum) evt.getNewValue();
            callShellWith( collatedStatus);
        } else if ("firingAlerts".equals(evt.getPropertyName())) {
            FiringAlertMeta[] alerts = (FiringAlertMeta[]) evt.getNewValue();

            Stream.of(alerts)
                    .collect(Collectors.toMap(
                            FiringAlertMeta::name,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ))
                    .values()
                    .forEach(this::callShellWith);

        } else {
            logger.warn("Got unexpected event {}", evt.getPropertyName());
        }
    }


    private void callShellWith(FiringAlertMeta firingAlertMeta) {
        if ( firingAlertMeta.resourceKey()==null) {
            logger.warn("Not firing alert due to missing resource key. AlertMeta: {}", firingAlertMeta);
            return;
        }
        logger.debug("Firing to file {}",firingAlertMeta);
        String[] cmd = new String[]{
                shellCommand,
                "alert",
                firingAlertMeta.resourceKey(),
                firingAlertMeta.name(),
                firingAlertMeta.description()
        };
        processExec(cmd);
    }

    private void callShellWith(EndpointCallEnum collatedStatus) {
        String[] cmd = new String[]{
                shellCommand,
                "status",
                collatedStatus.name()
        };
        processExec(cmd);
    }

    private static void processExec(String[] cmd) {
        if ( !new File(cmd[0]).canExecute() ){
            logger.error("Cannot find file to execute: {}", cmd[0]);
            return;
        }
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
        } catch ( InterruptedException e) {
            logger.error("Got unexpectedly interrupted", e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logger.error("Unexpected exception calling command {}", List.of(cmd), e);
        }
    }
}
