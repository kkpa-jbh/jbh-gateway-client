package com.jbh.gateway.internal;

import com.jbh.gateway.internal.core.http.exception.HttpClientException;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpMethod;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import com.jbh.gateway.client.JbhHttpResponse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * High-level gateway client interface for making HTTP requests to the JBH gateway service.
 * Provides convenient methods for different types of requests with automatic URL construction,
 * header propagation, and both string and typed response handling.
 *
 * <p>This is the primary interface that consuming microservices should interact with.
 * It abstracts away the underlying HTTP client implementation details and provides
 * a clean, domain-focused API.
 */
public interface JbhGatewayInternalClient extends AutoCloseable {

    // GET request methods

    /**
     * Performs a GET request to the specified path.
     *
     * @param path the path to append to the base URL
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse get(String path);

    /**
     * Performs a GET request to the specified path with custom headers.
     *
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse get(String path, JbhHttpHeaders headers);

    /**
     * Performs a GET request to the specified path with custom headers and timeout.
     *
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @param timeout request timeout duration
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse get(String path, JbhHttpHeaders headers, Duration timeout);

    /**
     * Performs a GET request and deserializes the response body to the specified type.
     *
     * @param <T> the type to deserialize the response body to
     * @param path the path to append to the base URL
     * @param responseType the class of the response type
     * @return the HTTP response with deserialized body
     * @throws HttpClientException if the request fails
     */
    <T> JbhHttpResponse getForObject(String path, Class<T> responseType);

    /**
     * Performs a GET request with custom headers and deserializes the response body to the specified type.
     *
     * @param <T> the type to deserialize the response body to
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @param responseType the class of the response type
     * @return the HTTP response with deserialized body
     * @throws HttpClientException if the request fails
     */
    <T> JbhHttpResponse getForObject(String path, JbhHttpHeaders headers, Class<T> responseType);

    // POST request methods

    /**
     * Performs a POST request to the specified path with JSON body.
     *
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse post(String path, String jsonBody);

    /**
     * Performs a POST request to the specified path with JSON body and custom headers.
     *
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse post(String path, String jsonBody, JbhHttpHeaders headers);

    /**
     * Performs a POST request to the specified path with JSON body, custom headers and timeout.
     *
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @param timeout request timeout duration
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse post(String path, String jsonBody, JbhHttpHeaders headers, Duration timeout);

    /**
     * Performs a POST request with JSON body and deserializes the response body to the specified type.
     *
     * @param <T> the type to deserialize the response body to
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param responseType the class of the response type
     * @return the HTTP response with deserialized body
     * @throws HttpClientException if the request fails
     */
    <T> JbhHttpResponse postForObject(String path, String jsonBody, Class<T> responseType);

    /**
     * Performs a POST request with JSON body and custom headers, deserializing the response body to the specified type.
     *
     * @param <T> the type to deserialize the response body to
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @param responseType the class of the response type
     * @return the HTTP response with deserialized body
     * @throws HttpClientException if the request fails
     */
    <T> JbhHttpResponse postForObject(String path, String jsonBody, JbhHttpHeaders headers, Class<T> responseType);

    // PUT request methods

    /**
     * Performs a PUT request to the specified path with JSON body.
     *
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse put(String path, String jsonBody);

    /**
     * Performs a PUT request to the specified path with JSON body and custom headers.
     *
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse put(String path, String jsonBody, JbhHttpHeaders headers);

    /**
     * Performs a PUT request to the specified path with JSON body, custom headers and timeout.
     *
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @param timeout request timeout duration
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse put(String path, String jsonBody, JbhHttpHeaders headers, Duration timeout);

    // DELETE request methods

    /**
     * Performs a DELETE request to the specified path.
     *
     * @param path the path to append to the base URL
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse delete(String path);

    /**
     * Performs a DELETE request to the specified path with custom headers.
     *
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse delete(String path, JbhHttpHeaders headers);

    /**
     * Performs a DELETE request to the specified path with custom headers and timeout.
     *
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @param timeout request timeout duration
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse delete(String path, JbhHttpHeaders headers, Duration timeout);

    // Async methods

    /**
     * Performs an asynchronous GET request to the specified path.
     *
     * @param path the path to append to the base URL
     * @return a CompletableFuture that will complete with the HTTP response
     */
    CompletableFuture<JbhHttpResponse> getAsync(String path);

    /**
     * Performs an asynchronous GET request to the specified path with custom headers.
     *
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @return a CompletableFuture that will complete with the HTTP response
     */
    CompletableFuture<JbhHttpResponse> getAsync(String path, JbhHttpHeaders headers);

    /**
     * Performs an asynchronous POST request to the specified path with JSON body.
     *
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @return a CompletableFuture that will complete with the HTTP response
     */
    CompletableFuture<JbhHttpResponse> postAsync(String path, String jsonBody);

    /**
     * Performs an asynchronous POST request to the specified path with JSON body and custom headers.
     *
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @return a CompletableFuture that will complete with the HTTP response
     */
    CompletableFuture<JbhHttpResponse> postAsync(String path, String jsonBody, JbhHttpHeaders headers);

    // Utility methods

    /**
     * Executes a custom HTTP request. This method provides full control over the request.
     *
     * @param request the HTTP request to execute
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    JbhHttpResponse execute(JbhHttpRequest request);

    /**
     * Executes a custom HTTP request asynchronously.
     *
     * @param request the HTTP request to execute
     * @return a CompletableFuture that will complete with the HTTP response
     */
    CompletableFuture<JbhHttpResponse> executeAsync(JbhHttpRequest request);

    /**
     * Executes a custom HTTP request and deserializes the response body to the specified type.
     *
     * @param <T> the type to deserialize the response body to
     * @param request the HTTP request to execute
     * @param responseType the class of the response type
     * @return the HTTP response with deserialized body
     * @throws HttpClientException if the request fails
     */
    <T> JbhHttpResponse executeForObject(JbhHttpRequest request, Class<T> responseType);

    /**
     * Creates a request builder for the specified method and path. This provides a fluent API for
     * building complex requests.
     *
     * @param method the HTTP method
     * @param path the path to append to the base URL
     * @return a request builder
     */
    JbhHttpRequest.Builder request(JbhHttpMethod method, String path);

    /**
     * Gets the base URL configured for this client.
     *
     * @return the base URL
     */
    String getBaseUrl();

    /**
     * Gets the name of the HTTP client adapter being used.
     *
     * @return the adapter name
     */
    String getClientAdapterName();

    /**
     * Checks if the underlying HTTP client supports HTTP/2.
     *
     * @return true if HTTP/2 is supported
     */
    boolean supportsHttp2();
}