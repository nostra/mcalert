package io.github.nostra.mcalert.exception;

public class McException extends RuntimeException {
    public McException(String errorMessage) {
        super(errorMessage);
    }

    public McException(String errorMessage, Exception e) {
        super(errorMessage, e);
    }
}
