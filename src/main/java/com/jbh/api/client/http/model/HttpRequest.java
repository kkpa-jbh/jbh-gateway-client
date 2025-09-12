package com.jbh.api.client.http.model;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of an HTTP request.
 * Contains all necessary information to execute an HTTP request including
 * method, URI, headers, body, and timeout configuration.
 */
public final class HttpRequest {

    private final HttpMethod method;
    private final URI uri;
    private final HttpHeaders headers;
    private final Optional<String> body;
    private final Duration timeout;

    private HttpRequest(HttpMethod method, URI uri, HttpHeaders headers, Optional<String> body, Duration timeout) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
        this.timeout = timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder get(String uri) {
        return builder().method(HttpMethod.GET).uri(URI.create(uri));
    }

    public static Builder get(URI uri) {
        return builder().method(HttpMethod.GET).uri(uri);
    }

    public static Builder post(String uri) {
        return builder().method(HttpMethod.POST).uri(URI.create(uri));
    }

    public static Builder post(URI uri) {
        return builder().method(HttpMethod.POST).uri(uri);
    }

    public static Builder put(String uri) {
        return builder().method(HttpMethod.PUT).uri(URI.create(uri));
    }

    public static Builder put(URI uri) {
        return builder().method(HttpMethod.PUT).uri(uri);
    }

    public static Builder delete(String uri) {
        return builder().method(HttpMethod.DELETE).uri(URI.create(uri));
    }

    public static Builder delete(URI uri) {
        return builder().method(HttpMethod.DELETE).uri(uri);
    }

    public HttpMethod getMethod() {
        return method;
    }

    public URI getUri() {
        return uri;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public Optional<String> getBody() {
        return body;
    }

    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpRequest that = (HttpRequest) o;
        return method == that.method &&
                Objects.equals(uri, that.uri) &&
                Objects.equals(headers, that.headers) &&
                Objects.equals(body, that.body) &&
                Objects.equals(timeout, that.timeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, uri, headers, body, timeout);
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "method=" + method +
                ", uri=" + uri +
                ", headers=" + headers +
                ", body=" + body.map(b -> b.length() > 100 ? b.substring(0, 100) + "..." : b) +
                ", timeout=" + timeout +
                '}';
    }

    public static class Builder {
        private HttpMethod method;
        private URI uri;
        private HttpHeaders.Builder headersBuilder = HttpHeaders.builder();
        private String body;
        private Duration timeout = Duration.ofSeconds(30); // Default 30 second timeout

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = URI.create(uri);
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

        public Builder jsonBody(String jsonBody) {
            this.body = jsonBody;
            headersBuilder.set("Content-Type", "application/json");
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public HttpRequest build() {
            Objects.requireNonNull(method, "Method cannot be null");
            Objects.requireNonNull(uri, "URI cannot be null");
            Objects.requireNonNull(timeout, "Timeout cannot be null");

            return new HttpRequest(
                    method,
                    uri,
                    headersBuilder.build(),
                    Optional.ofNullable(body),
                    timeout
            );
        }
    }
}