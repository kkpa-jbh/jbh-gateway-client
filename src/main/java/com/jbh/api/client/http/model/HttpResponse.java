package com.jbh.api.client.http.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of an HTTP response.
 * Contains status code, headers, and response body.
 * Provides utility methods for common response operations.
 */
public final class HttpResponse {

    private final int statusCode;
    private final HttpHeaders headers;
    private final Optional<String> body;

    private HttpResponse(int statusCode, HttpHeaders headers, Optional<String> body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public static HttpResponse of(int statusCode, HttpHeaders headers, String body) {
        return new HttpResponse(statusCode, headers, Optional.ofNullable(body));
    }

    public static HttpResponse of(int statusCode, HttpHeaders headers) {
        return new HttpResponse(statusCode, headers, Optional.empty());
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public Optional<String> getBody() {
        return body;
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
        HttpResponse that = (HttpResponse) o;
        return statusCode == that.statusCode &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, headers, body);
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
        private HttpHeaders.Builder headersBuilder = HttpHeaders.builder();
        private String body;

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder header(String name, String value) {
            headersBuilder.add(name, value);
            return this;
        }

        public Builder headers(HttpHeaders headers) {
            headersBuilder.addAll(headers);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(statusCode, headersBuilder.build(), Optional.ofNullable(body));
        }
    }
}