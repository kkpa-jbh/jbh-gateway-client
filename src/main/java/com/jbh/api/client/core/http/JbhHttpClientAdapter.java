package com.jbh.api.client.core.http;

import com.jbh.api.client.core.http.exception.HttpClientException;
import com.jbh.api.client.core.http.model.JbhHttpRequest;
import com.jbh.api.client.core.http.model.JbhHttpResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Core interface for HTTP client adapters.
 * Defines the contract that all HTTP client implementations must follow.
 * Provides both synchronous and asynchronous methods for maximum flexibility.
 * 
 * This interface allows the gateway client to be agnostic of the underlying
 * HTTP client implementation (Native Java HTTP Client, OkHttp, etc.).
 */
public interface JbhHttpClientAdapter extends AutoCloseable {

    /**
     * Execute an HTTP request synchronously.
     * 
     * @param request the HTTP request to execute
     * @return the HTTP response
     * @throws HttpClientException for any HTTP client related errors
     */
    JbhHttpResponse execute(JbhHttpRequest request);

    /**
     * Execute an HTTP request asynchronously.
     * 
     * @param request the HTTP request to execute
     * @return a CompletableFuture that will complete with the HTTP response
     */
    CompletableFuture<JbhHttpResponse> executeAsync(JbhHttpRequest request);

    /**
     * Get the name/type of this HTTP client adapter.
     * Useful for logging, debugging, and metrics collection.
     * 
     * @return the adapter name (e.g., "native", "okhttp")
     */
    String getAdapterName();

    /**
     * Check if this adapter supports HTTP/2.
     * 
     * @return true if HTTP/2 is supported, false otherwise
     */
    boolean supportsHttp2();

    /**
     * Close the HTTP client and release any resources.
     * This method should be idempotent and safe to call multiple times.
     */
    @Override
    void close();
}