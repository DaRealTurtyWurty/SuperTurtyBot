package dev.darealturtywurty.superturtybot.dashboard.http;

import io.javalin.http.HttpStatus;

public final class DashboardApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public DashboardApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return this.errorCode;
    }

    public HttpStatus status() {
        return this.status;
    }
}
