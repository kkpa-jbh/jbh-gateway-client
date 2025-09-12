package com.jbh.api.client.core.http.exception;

/**
 * Exception thrown when HTTP requests timeout.
 * This provides specific handling for timeout scenarios which are
 * common in network operations and may require different retry strategies.
 */
public class HttpTimeoutException extends HttpClientException {

    private final long timeoutMillis;

    public HttpTimeoutException(String message, long timeoutMillis) {
        super(message, "TIMEOUT");
        this.timeoutMillis = timeoutMillis;
    }

    public HttpTimeoutException(String message, long timeoutMillis, Throwable cause) {
        super(message, "TIMEOUT", null, cause);
        this.timeoutMillis = timeoutMillis;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
}