package io.github.nostra.mcalert.tray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class PollPrometheus {
    private static final Logger logger = LoggerFactory.getLogger(PollPrometheus.class);

    private final URL prometheusEndpoint;

    public PollPrometheus(URL prometheusEndpoint ) {
        this.prometheusEndpoint = prometheusEndpoint;
    }

    public void poll() {

    }
}
