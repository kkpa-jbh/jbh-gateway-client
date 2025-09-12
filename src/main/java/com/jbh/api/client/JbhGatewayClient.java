package com.jbh.api.client;

import com.jbh.api.client.config.HttpClientConfig;
import com.jbh.api.client.http.JbhHttpClientAdapter;
import com.jbh.api.client.http.JbhHttpClientFactory;
import com.jbh.api.client.http.model.JbhHttpHeaders;
import com.jbh.api.client.http.model.JbhHttpMethod;
import com.jbh.api.client.http.model.JbhHttpRequest;
import com.jbh.api.client.http.model.JbhHttpResponse;
import com.jbh.api.client.http.exception.HttpClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Main gateway client service that provides a high-level API for making HTTP requests
 * to the JBH gateway service. This class encapsulates the HTTP client configuration,
 * connection management, and provides convenient methods for different types of requests.
 * 
 * This is the primary class that consuming microservices will interact with.
 * It handles URL construction, header propagation, and integrates with the underlying
 * HTTP client adapters.
 */
public class JbhGatewayClient implements AutoCloseable {
    
    private static final Logger log = LoggerFactory.getLogger(JbhGatewayClient.class);
    
    private final String baseUrl;
    private final JbhHttpClientAdapter httpClient;
    private final JbhHttpClientFactory httpClientFactory;
    private final Duration defaultTimeout;
    
    /**
     * Constructs a gateway client with the specified configuration.
     * 
     * @param baseUrl the base URL of the gateway service
     * @param httpClientType the type of HTTP client adapter to use ("native", "okhttp")
     * @param httpClientConfig the HTTP client configuration
     * @throws IllegalArgumentException if parameters are invalid
     */
    public JbhGatewayClient(String baseUrl, String httpClientType, HttpClientConfig httpClientConfig) {
        this.baseUrl = validateAndNormalizeBaseUrl(baseUrl);
        this.httpClientFactory = new JbhHttpClientFactory(httpClientConfig);
        this.httpClient = httpClientFactory.createAdapter(httpClientType, httpClientConfig);
        this.defaultTimeout = httpClientConfig.getRequestTimeout();
        
        log.info("Gateway client initialized with base URL: {}, client type: {}", this.baseUrl, httpClientType);
    }
    
    /**
     * Constructs a gateway client with default configuration.
     * 
     * @param baseUrl the base URL of the gateway service
     */
    public JbhGatewayClient(String baseUrl) {
        this(baseUrl, "native", HttpClientConfig.defaultConfig());
    }
    
    // GET request methods
    
    /**
     * Performs a GET request to the specified path.
     * 
     * @param path the path to append to the base URL
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse get(String path) {
        return get(path, JbhHttpHeaders.empty());
    }
    
    /**
     * Performs a GET request to the specified path with custom headers.
     * 
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse get(String path, JbhHttpHeaders headers) {
        return get(path, headers, defaultTimeout);
    }
    
    /**
     * Performs a GET request to the specified path with custom headers and timeout.
     * 
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @param timeout request timeout duration
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse get(String path, JbhHttpHeaders headers, Duration timeout) {
        JbhHttpRequest request = JbhHttpRequest.get(buildUrl(path))
                .headers(headers)
                .timeout(timeout)
                .build();
                
        log.debug("Executing GET request to: {}", request.getUri());
        return httpClient.execute(request);
    }
    
    // POST request methods
    
    /**
     * Performs a POST request to the specified path with JSON body.
     * 
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse post(String path, String jsonBody) {
        return post(path, jsonBody, JbhHttpHeaders.empty());
    }
    
    /**
     * Performs a POST request to the specified path with JSON body and custom headers.
     * 
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse post(String path, String jsonBody, JbhHttpHeaders headers) {
        return post(path, jsonBody, headers, defaultTimeout);
    }
    
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
    public JbhHttpResponse post(String path, String jsonBody, JbhHttpHeaders headers, Duration timeout) {
        JbhHttpRequest request = JbhHttpRequest.post(buildUrl(path))
                .jsonBody(jsonBody)
                .headers(headers)
                .timeout(timeout)
                .build();
                
        log.debug("Executing POST request to: {}", request.getUri());
        return httpClient.execute(request);
    }
    
    // PUT request methods
    
    /**
     * Performs a PUT request to the specified path with JSON body.
     * 
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse put(String path, String jsonBody) {
        return put(path, jsonBody, JbhHttpHeaders.empty());
    }
    
    /**
     * Performs a PUT request to the specified path with JSON body and custom headers.
     * 
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse put(String path, String jsonBody, JbhHttpHeaders headers) {
        return put(path, jsonBody, headers, defaultTimeout);
    }
    
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
    public JbhHttpResponse put(String path, String jsonBody, JbhHttpHeaders headers, Duration timeout) {
        JbhHttpRequest request = JbhHttpRequest.put(buildUrl(path))
                .jsonBody(jsonBody)
                .headers(headers)
                .timeout(timeout)
                .build();
                
        log.debug("Executing PUT request to: {}", request.getUri());
        return httpClient.execute(request);
    }
    
    // DELETE request methods
    
    /**
     * Performs a DELETE request to the specified path.
     * 
     * @param path the path to append to the base URL
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse delete(String path) {
        return delete(path, JbhHttpHeaders.empty());
    }
    
    /**
     * Performs a DELETE request to the specified path with custom headers.
     * 
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse delete(String path, JbhHttpHeaders headers) {
        return delete(path, headers, defaultTimeout);
    }
    
    /**
     * Performs a DELETE request to the specified path with custom headers and timeout.
     * 
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @param timeout request timeout duration
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse delete(String path, JbhHttpHeaders headers, Duration timeout) {
        JbhHttpRequest request = JbhHttpRequest.delete(buildUrl(path))
                .headers(headers)
                .timeout(timeout)
                .build();
                
        log.debug("Executing DELETE request to: {}", request.getUri());
        return httpClient.execute(request);
    }
    
    // Async methods
    
    /**
     * Performs an asynchronous GET request to the specified path.
     * 
     * @param path the path to append to the base URL
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<JbhHttpResponse> getAsync(String path) {
        return getAsync(path, JbhHttpHeaders.empty());
    }
    
    /**
     * Performs an asynchronous GET request to the specified path with custom headers.
     * 
     * @param path the path to append to the base URL
     * @param headers additional headers to include in the request
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<JbhHttpResponse> getAsync(String path, JbhHttpHeaders headers) {
        JbhHttpRequest request = JbhHttpRequest.get(buildUrl(path))
                .headers(headers)
                .timeout(defaultTimeout)
                .build();
                
        log.debug("Executing async GET request to: {}", request.getUri());
        return httpClient.executeAsync(request);
    }
    
    /**
     * Performs an asynchronous POST request to the specified path with JSON body.
     * 
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<JbhHttpResponse> postAsync(String path, String jsonBody) {
        return postAsync(path, jsonBody, JbhHttpHeaders.empty());
    }
    
    /**
     * Performs an asynchronous POST request to the specified path with JSON body and custom headers.
     * 
     * @param path the path to append to the base URL
     * @param jsonBody the JSON request body
     * @param headers additional headers to include in the request
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<JbhHttpResponse> postAsync(String path, String jsonBody, JbhHttpHeaders headers) {
        JbhHttpRequest request = JbhHttpRequest.post(buildUrl(path))
                .jsonBody(jsonBody)
                .headers(headers)
                .timeout(defaultTimeout)
                .build();
                
        log.debug("Executing async POST request to: {}", request.getUri());
        return httpClient.executeAsync(request);
    }
    
    // Utility methods
    
    /**
     * Executes a custom HTTP request. This method provides full control over the request.
     * 
     * @param request the HTTP request to execute
     * @return the HTTP response
     * @throws HttpClientException if the request fails
     */
    public JbhHttpResponse execute(JbhHttpRequest request) {
        log.debug("Executing custom request: {} {}", request.getMethod(), request.getUri());
        return httpClient.execute(request);
    }
    
    /**
     * Executes a custom HTTP request asynchronously.
     * 
     * @param request the HTTP request to execute
     * @return a CompletableFuture that will complete with the HTTP response
     */
    public CompletableFuture<JbhHttpResponse> executeAsync(JbhHttpRequest request) {
        log.debug("Executing async custom request: {} {}", request.getMethod(), request.getUri());
        return httpClient.executeAsync(request);
    }
    
    /**
     * Creates a request builder for the specified method and path.
     * This provides a fluent API for building complex requests.
     * 
     * @param method the HTTP method
     * @param path the path to append to the base URL
     * @return a request builder
     */
    public JbhHttpRequest.Builder request(JbhHttpMethod method, String path) {
        return JbhHttpRequest.builder()
                .method(method)
                .uri(buildUrl(path))
                .timeout(defaultTimeout);
    }
    
    /**
     * Gets the base URL configured for this client.
     * 
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Gets the name of the HTTP client adapter being used.
     * 
     * @return the adapter name
     */
    public String getClientAdapterName() {
        return httpClient.getAdapterName();
    }
    
    /**
     * Checks if the underlying HTTP client supports HTTP/2.
     * 
     * @return true if HTTP/2 is supported
     */
    public boolean supportsHttp2() {
        return httpClient.supportsHttp2();
    }
    
    @Override
    public void close() {
        log.info("Closing gateway client");
        try {
            httpClient.close();
        } catch (Exception e) {
            log.warn("Error closing HTTP client adapter: {}", e.getMessage(), e);
        }
        
        try {
            httpClientFactory.shutdown();
        } catch (Exception e) {
            log.warn("Error shutting down HTTP client factory: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Validates and normalizes the base URL.
     */
    private String validateAndNormalizeBaseUrl(String baseUrl) {
        Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("Base URL cannot be blank");
        }
        
        try {
            URI uri = URI.create(baseUrl);
            if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                throw new IllegalArgumentException("Base URL must have http or https scheme");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base URL format: " + baseUrl, e);
        }
        
        // Remove trailing slash for consistency
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
    
    /**
     * Builds a complete URL by combining the base URL with the given path.
     */
    private String buildUrl(String path) {
        if (path == null) {
            return baseUrl;
        }
        
        // Ensure path starts with /
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return baseUrl + normalizedPath;
    }
}