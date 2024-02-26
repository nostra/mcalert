package io.github.nostra.mcalert.client;

import io.github.nostra.mcalert.model.PrometheusResult;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class SingleEndpointPoller {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private AlertCaller caller;
    private int numAlerts = -1;

    public SingleEndpointPoller(URI uri) {
        caller = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build(AlertCaller.class);

    }

    // For junit tests
    SingleEndpointPoller() {
        // Intentionally empty
    }

    public PrometheusResult callPrometheus() {
        // TODO To create alerting setup here

        PrometheusResult result = caller.callPrometheus();
        int current = result.noAlerts() ? 0 : result.data().alerts().size();
        pcs.firePropertyChange("numAlerts", numAlerts, current);
        numAlerts = current;

        return result;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }
}
