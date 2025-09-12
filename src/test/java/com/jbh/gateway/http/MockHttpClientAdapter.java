package com.jbh.gateway.http;

import com.jbh.gateway.internal.core.mappers.JacksonJsonUtil;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import com.jbh.gateway.internal.core.http.model.JbhHttpResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Mock implementation of HttpClientAdapter for testing purposes.
 * Allows tests to simulate various HTTP responses and error conditions
 * without making actual network calls.
 */
public class MockHttpClientAdapter implements JbhHttpClientAdapter {

    private Function<JbhHttpRequest, JbhHttpResponse> responseProvider;
    private Function<JbhHttpRequest, RuntimeException> exceptionProvider;
    private boolean closed = false;

    public MockHttpClientAdapter() {
        this.responseProvider = this::defaultResponse;
    }

    public MockHttpClientAdapter(Function<JbhHttpRequest, JbhHttpResponse> responseProvider) {
        this.responseProvider = responseProvider;
    }

    public static MockHttpClientAdapter withResponse(JbhHttpResponse response) {
        return new MockHttpClientAdapter(request -> response);
    }

    public static MockHttpClientAdapter withStatusCode(int statusCode) {
        JbhHttpResponse response = JbhHttpResponse.builder()
                .statusCode(statusCode)
                .headers(JbhHttpHeaders.builder()
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

    public MockHttpClientAdapter thenReturn(JbhHttpResponse response) {
        this.responseProvider = request -> response;
        return this;
    }

    public MockHttpClientAdapter thenThrow(RuntimeException exception) {
        this.exceptionProvider = request -> exception;
        return this;
    }

    @Override
    public JbhHttpResponse execute(JbhHttpRequest request) {
        if (closed) {
            throw new IllegalStateException("HttpClientAdapter has been closed");
        }

        if (exceptionProvider != null) {
            throw exceptionProvider.apply(request);
        }

        return responseProvider.apply(request);
    }

    @Override
    public CompletableFuture<JbhHttpResponse> executeAsync(JbhHttpRequest request) {
        try {
            JbhHttpResponse response = execute(request);
            return CompletableFuture.completedFuture(response);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public <T> JbhHttpResponse executeForObject(JbhHttpRequest request, Class<T> responseType) {
        if (closed) {
            throw new IllegalStateException("HttpClientAdapter has been closed");
        }

        if (exceptionProvider != null) {
            throw exceptionProvider.apply(request);
        }

        JbhHttpResponse stringResponse = responseProvider.apply(request);
        return convertToTypedResponse(stringResponse, responseType);
    }

    @Override
    public <T> CompletableFuture<JbhHttpResponse> executeAsyncForObject(JbhHttpRequest request, Class<T> responseType) {
        try {
            JbhHttpResponse response = executeForObject(request, responseType);
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

    private <T> JbhHttpResponse convertToTypedResponse(JbhHttpResponse stringResponse, Class<T> responseType) {
        // Only attempt JSON deserialization for successful responses with a body
        if (stringResponse.isSuccessful() && stringResponse.getBody().isPresent() && !stringResponse.getBody().get().isEmpty()) {
            try {
                T typedBody = JacksonJsonUtil.fromJson(stringResponse.getBody().get(), responseType);
                return JbhHttpResponse.ofTyped(
                    stringResponse.getStatusCode(), 
                    stringResponse.getHeaders(), 
                    stringResponse.getBody().get(), 
                    typedBody);
            } catch (Exception e) {
                // Fall back to original response if deserialization fails
                return stringResponse;
            }
        }
        return stringResponse;
    }

    private JbhHttpResponse defaultResponse(JbhHttpRequest request) {
        return JbhHttpResponse.builder()
                .statusCode(200)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .add("X-Mock-Response", "true")
                        .build())
                .body("{\"method\":\"" + request.getMethod() + "\",\"url\":\"" + request.getUri() + "\"}")
                .build();
    }
}