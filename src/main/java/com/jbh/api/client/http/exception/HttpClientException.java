package com.jbh.api.client.http.exception;

/**
 * Base exception for all HTTP client related errors.
 * Provides a common parent for all HTTP client exceptions to enable
 * consistent error handling across different HTTP client implementations.
 */
public class HttpClientException extends RuntimeException {

    private final String errorCode;
    private final Integer httpStatusCode;

    public HttpClientException(String message) {
        super(message);
        this.errorCode = null;
        this.httpStatusCode = null;
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.httpStatusCode = null;
    }

    public HttpClientException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatusCode = null;
    }

    public HttpClientException(String message, String errorCode, Integer httpStatusCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public HttpClientException(String message, String errorCode, Integer httpStatusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}