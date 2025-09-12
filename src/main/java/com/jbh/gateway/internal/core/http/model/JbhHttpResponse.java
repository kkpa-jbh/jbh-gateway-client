package com.jbh.gateway.internal.core.http.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of an HTTP response.
 * Contains status code, headers, and response body.
 * Provides utility methods for common response operations.
 */
public final class JbhHttpResponse {

    private final int statusCode;
    private final JbhHttpHeaders headers;
    private final Optional<String> body;
    private final Optional<Object> typedBody;

    private JbhHttpResponse(int statusCode, JbhHttpHeaders headers, Optional<String> body, Optional<Object> typedBody) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.typedBody = typedBody;
    }

    public static JbhHttpResponse of(int statusCode, JbhHttpHeaders headers, String body) {
        return new JbhHttpResponse(statusCode, headers, Optional.ofNullable(body), Optional.empty());
    }

    public static JbhHttpResponse of(int statusCode, JbhHttpHeaders headers) {
        return new JbhHttpResponse(statusCode, headers, Optional.empty(), Optional.empty());
    }

    public static <T> JbhHttpResponse ofTyped(int statusCode, JbhHttpHeaders headers, String body, T typedBody) {
        return new JbhHttpResponse(statusCode, headers, Optional.ofNullable(body), Optional.ofNullable(typedBody));
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public JbhHttpHeaders getHeaders() {
        return headers;
    }

    public Optional<String> getBody() {
        return body;
    }

    /**
     * Get the response body as the specified type.
     * 
     * @param <T> the expected type
     * @param clazz the class of the expected type
     * @return the typed body if available and of the correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getBodyAs(Class<T> clazz) {
        return typedBody.filter(obj -> clazz.isInstance(obj))
                .map(obj -> (T) obj);
    }

    /**
     * Check if this response has a typed body.
     * 
     * @return true if a typed body is present
     */
    public boolean hasTypedBody() {
        return typedBody.isPresent();
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    public boolean isServerError() {
        return statusCode >= 500;
    }

    public boolean isRedirection() {
        return statusCode >= 300 && statusCode < 400;
    }

    public Optional<String> getContentType() {
        return headers.getFirst("Content-Type");
    }

    public Optional<Long> getContentLength() {
        return headers.getFirst("Content-Length")
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JbhHttpResponse that = (JbhHttpResponse) o;
        return statusCode == that.statusCode &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(body, that.body) &&
                Objects.equals(typedBody, that.typedBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, headers, body, typedBody);
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "statusCode=" + statusCode +
                ", headers=" + headers +
                ", body=" + body.map(b -> b.length() > 100 ? b.substring(0, 100) + "..." : b) +
                '}';
    }

    public static class Builder {
        private int statusCode;
        private JbhHttpHeaders.Builder headersBuilder = JbhHttpHeaders.builder();
        private String body;
        private Object typedBody;

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder header(String name, String value) {
            headersBuilder.add(name, value);
            return this;
        }

        public Builder headers(JbhHttpHeaders headers) {
            headersBuilder.addAll(headers);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public <T> Builder typedBody(T typedBody) {
            this.typedBody = typedBody;
            return this;
        }

        public JbhHttpResponse build() {
            return new JbhHttpResponse(statusCode, headersBuilder.build(), Optional.ofNullable(body), Optional.ofNullable(typedBody));
        }
    }
}