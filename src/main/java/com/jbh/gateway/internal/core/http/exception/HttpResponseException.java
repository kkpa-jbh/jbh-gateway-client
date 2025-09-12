package com.jbh.gateway.internal.core.http.exception;

/**
 * Exception thrown when HTTP requests receive error responses (4xx, 5xx status codes).
 * Provides access to the response status code and body for detailed error handling.
 */
public class HttpResponseException extends HttpClientException {

    private final String responseBody;

    public HttpResponseException(String message, int statusCode, String responseBody) {
        super(message, "HTTP_ERROR", statusCode);
        this.responseBody = responseBody;
    }

    public HttpResponseException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, "HTTP_ERROR", statusCode, cause);
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isClientError() {
        return getHttpStatusCode() != null && getHttpStatusCode() >= 400 && getHttpStatusCode() < 500;
    }

    public boolean isServerError() {
        return getHttpStatusCode() != null && getHttpStatusCode() >= 500;
    }
}