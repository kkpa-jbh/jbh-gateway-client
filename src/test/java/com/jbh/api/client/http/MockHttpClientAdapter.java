package com.jbh.api.client.http;

import com.jbh.api.client.http.model.HttpHeaders;
import com.jbh.api.client.http.model.HttpRequest;
import com.jbh.api.client.http.model.HttpResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Mock implementation of HttpClientAdapter for testing purposes.
 * Allows tests to simulate various HTTP responses and error conditions
 * without making actual network calls.
 */
public class MockHttpClientAdapter implements HttpClientAdapter {

    private Function<HttpRequest, HttpResponse> responseProvider;
    private Function<HttpRequest, RuntimeException> exceptionProvider;
    private boolean closed = false;

    public MockHttpClientAdapter() {
        this.responseProvider = this::defaultResponse;
    }

    public MockHttpClientAdapter(Function<HttpRequest, HttpResponse> responseProvider) {
        this.responseProvider = responseProvider;
    }

    public static MockHttpClientAdapter withResponse(HttpResponse response) {
        return new MockHttpClientAdapter(request -> response);
    }

    public static MockHttpClientAdapter withStatusCode(int statusCode) {
        HttpResponse response = HttpResponse.builder()
                .statusCode(statusCode)
                .headers(HttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body("{\"message\":\"Mock response\"}")
                .build();
        return withResponse(response);
    }

    public static MockHttpClientAdapter withException(RuntimeException exception) {
        MockHttpClientAdapter adapter = new MockHttpClientAdapter();
        adapter.exceptionProvider = request -> exception;
        return adapter;
    }

    public MockHttpClientAdapter thenReturn(HttpResponse response) {
        this.responseProvider = request -> response;
        return this;
    }

    public MockHttpClientAdapter thenThrow(RuntimeException exception) {
        this.exceptionProvider = request -> exception;
        return this;
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        if (closed) {
            throw new IllegalStateException("HttpClientAdapter has been closed");
        }

        if (exceptionProvider != null) {
            throw exceptionProvider.apply(request);
        }

        return responseProvider.apply(request);
    }

    @Override
    public CompletableFuture<HttpResponse> executeAsync(HttpRequest request) {
        try {
            HttpResponse response = execute(request);
            return CompletableFuture.completedFuture(response);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String getAdapterName() {
        return "mock";
    }

    @Override
    public boolean supportsHttp2() {
        return true;
    }

    @Override
    public void close() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    private HttpResponse defaultResponse(HttpRequest request) {
        return HttpResponse.builder()
                .statusCode(200)
                .headers(HttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .add("X-Mock-Response", "true")
                        .build())
                .body("{\"method\":\"" + request.getMethod() + "\",\"url\":\"" + request.getUri() + "\"}")
                .build();
    }
}