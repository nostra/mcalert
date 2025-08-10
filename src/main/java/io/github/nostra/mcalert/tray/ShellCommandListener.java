package io.github.nostra.mcalert.tray;

import io.github.nostra.mcalert.client.AlertResource;
import io.github.nostra.mcalert.client.EndpointCallEnum;
import io.github.nostra.mcalert.config.AlertEndpointConfig;
import io.quarkus.runtime.Shutdown;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;

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
            alertResource.addPropertyChangeListener(this);
            // TODO Might want to trigger on specific change, preferably with alert description
            // alertResource.map().values().forEach(singleEndpointPoller -> singleEndpointPoller.addPropertyChangeListener(this));
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
        //} else if ("firingAlerts".equals(evt.getPropertyName())) {
        //    logger.error("firingAlerts {}", evt.getPropertyName());
        } else {
            logger.warn("Got unexpected event {}", evt.getPropertyName());
        }

    }

    private void callShellWith(EndpointCallEnum collatedStatus) {
        String[] cmd = new String[]{
                shellCommand,
                "status",
                collatedStatus.name()
        };
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            //process.waitFor();
        } catch (IOException e) {
            logger.error("Unexpected exception calling command {}", List.of(cmd), e);
        }
    }
}
