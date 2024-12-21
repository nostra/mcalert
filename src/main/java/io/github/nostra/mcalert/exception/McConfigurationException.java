package io.github.nostra.mcalert.exception;

/// Exception signifying configuration problem(s)
public class McConfigurationException extends McException {
    public McConfigurationException(String msg) {
        super(msg);
    }
}
