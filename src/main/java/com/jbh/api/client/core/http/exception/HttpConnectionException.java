package com.jbh.api.client.core.http.exception;

/**
 * Exception thrown when HTTP connection establishment fails.
 * This includes network connectivity issues, DNS resolution failures,
 * and other connection-level problems.
 */
public class HttpConnectionException extends HttpClientException {

    private final String targetHost;

    public HttpConnectionException(String message, String targetHost) {
        super(message, "CONNECTION_FAILED");
        this.targetHost = targetHost;
    }

    public HttpConnectionException(String message, String targetHost, Throwable cause) {
        super(message, "CONNECTION_FAILED", null, cause);
        this.targetHost = targetHost;
    }

    public String getTargetHost() {
        return targetHost;
    }
}