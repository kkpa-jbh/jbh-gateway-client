package com.jbh.gateway.internal;

import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.core.http.AdapterType;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.JbhHttpClientFactory;
import com.jbh.gateway.internal.core.http.exception.HttpClientException;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpMethod;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import com.jbh.gateway.internal.core.http.model.JbhHttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main gateway client service that provides a high-level API for making HTTP requests to the JBH
 * gateway service. This class encapsulates the HTTP client configuration, connection management,
 * and provides convenient methods for different types of requests.
 *
 * <p>This is the primary class that consuming microservices will interact with. It handles URL
 * construction, header propagation, and integrates with the underlying HTTP client adapters.
 */
public class JbhGatewayClientImpl implements JbhGatewayClient {

  private static final Logger log = LoggerFactory.getLogger(JbhGatewayClientImpl.class);

  private final String baseUrl;
  private final JbhHttpClientAdapter jbhHttpClientAdapter;
  private final JbhHttpClientFactory jbhHttpClientFactory;
  private final Duration defaultTimeout;
  private JbhHttpHeaders defaultJbhHttpHeaders;

  /**
   * Constructs a gateway client with default configuration.
   *
   * @param baseUrl the base URL of the gateway service
   * @param adapterType the type of HTTP client adapter to use ("native", "okhttp")
   */
  public JbhGatewayClientImpl(String baseUrl, AdapterType adapterType) {
    this(baseUrl, adapterType, JbhHttpClientConfig.defaultConfig());
  }

  /**
   * Constructs a gateway client with the specified configuration.
   *
   * @param baseUrl the base URL of the gateway service
   * @param httpClientType the type of HTTP client adapter to use ("native", "okhttp")
   * @param httpClientConfig the HTTP client configuration
   * @throws IllegalArgumentException if parameters are invalid
   */
  public JbhGatewayClientImpl(
      String baseUrl, AdapterType httpClientType, JbhHttpClientConfig httpClientConfig) {
    this.baseUrl = validateAndNormalizeBaseUrl(baseUrl);
    this.jbhHttpClientFactory = new JbhHttpClientFactory(httpClientConfig);
    this.jbhHttpClientAdapter =
        jbhHttpClientFactory.createAdapter(httpClientType, httpClientConfig);
    this.defaultTimeout = httpClientConfig.getRequestTimeout();

    defaultJbhHttpHeaders = JbhHttpHeaders.empty(); // From Map
    if (httpClientConfig.getUserAgent().isPresent()) {
      defaultJbhHttpHeaders =
          defaultJbhHttpHeaders.with(
              JbhHttpHeaders.USER_AGENT_HEADER, httpClientConfig.getUserAgent().get());
    }
    if (httpClientConfig.getCustomHeaders() != null
        && !httpClientConfig.getCustomHeaders().isEmpty()) {
      defaultJbhHttpHeaders = defaultJbhHttpHeaders.withAll(httpClientConfig.getCustomHeaders());
    }

    log.info(
        "Gateway client initialized with base URL: {}, client type: {}",
        this.baseUrl,
        httpClientType);
  }

  // GET request methods

  /**
   * Performs a GET request to the specified path.
   *
   * @param path the path to append to the base URL
   * @return the HTTP response
   * @throws HttpClientException if the request fails
   */
  @Override
  public JbhHttpResponse get(String path) {
    return get(path, defaultJbhHttpHeaders);
  }

  /**
   * Performs a GET request to the specified path with custom headers.
   *
   * @param path the path to append to the base URL
   * @param headers additional headers to include in the request
   * @return the HTTP response
   * @throws HttpClientException if the request fails
   */
  @Override
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
  @Override
  public JbhHttpResponse get(String path, JbhHttpHeaders headers, Duration timeout) {
    JbhHttpRequest request =
        JbhHttpRequest.get(buildUrl(path)).headers(headers).timeout(timeout).build();

    log.debug("Executing GET request to: {}", request.getUri());
    return jbhHttpClientAdapter.execute(request);
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
  @Override
  public JbhHttpResponse post(String path, String jsonBody) {
    return post(path, jsonBody, defaultJbhHttpHeaders);
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
  @Override
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
  @Override
  public JbhHttpResponse post(
      String path, String jsonBody, JbhHttpHeaders headers, Duration timeout) {
    JbhHttpRequest request =
        JbhHttpRequest.post(buildUrl(path))
            .jsonBody(jsonBody)
            .headers(headers)
            .timeout(timeout)
            .build();

    log.debug("Executing POST request to: {}", request.getUri());
    return jbhHttpClientAdapter.execute(request);
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
  @Override
  public JbhHttpResponse put(String path, String jsonBody) {
    return put(path, jsonBody, defaultJbhHttpHeaders);
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
  @Override
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
  @Override
  public JbhHttpResponse put(
      String path, String jsonBody, JbhHttpHeaders headers, Duration timeout) {
    JbhHttpRequest request =
        JbhHttpRequest.put(buildUrl(path))
            .jsonBody(jsonBody)
            .headers(headers)
            .timeout(timeout)
            .build();

    log.debug("Executing PUT request to: {}", request.getUri());
    return jbhHttpClientAdapter.execute(request);
  }

  // DELETE request methods

  /**
   * Performs a DELETE request to the specified path.
   *
   * @param path the path to append to the base URL
   * @return the HTTP response
   * @throws HttpClientException if the request fails
   */
  @Override
  public JbhHttpResponse delete(String path) {
    return delete(path, defaultJbhHttpHeaders);
  }

  /**
   * Performs a DELETE request to the specified path with custom headers.
   *
   * @param path the path to append to the base URL
   * @param headers additional headers to include in the request
   * @return the HTTP response
   * @throws HttpClientException if the request fails
   */
  @Override
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
  @Override
  public JbhHttpResponse delete(String path, JbhHttpHeaders headers, Duration timeout) {
    JbhHttpRequest request =
        JbhHttpRequest.delete(buildUrl(path)).headers(headers).timeout(timeout).build();

    log.debug("Executing DELETE request to: {}", request.getUri());
    return jbhHttpClientAdapter.execute(request);
  }

  // Typed GET request methods

  /**
   * Performs a GET request and deserializes the response body to the specified type.
   *
   * @param <T> the type to deserialize the response body to
   * @param path the path to append to the base URL
   * @param responseType the class of the response type
   * @return the HTTP response with deserialized body
   * @throws HttpClientException if the request fails
   */
  @Override
  public <T> JbhHttpResponse getForObject(String path, Class<T> responseType) {
    return getForObject(path, defaultJbhHttpHeaders, responseType);
  }

  /**
   * Performs a GET request with custom headers and deserializes the response body to the specified
   * type.
   *
   * @param <T> the type to deserialize the response body to
   * @param path the path to append to the base URL
   * @param headers additional headers to include in the request
   * @param responseType the class of the response type
   * @return the HTTP response with deserialized body
   * @throws HttpClientException if the request fails
   */
  @Override
  public <T> JbhHttpResponse getForObject(
      String path, JbhHttpHeaders headers, Class<T> responseType) {
    JbhHttpRequest request =
        JbhHttpRequest.get(buildUrl(path)).headers(headers).timeout(defaultTimeout).build();

    log.debug(
        "Executing typed GET request to: {} for type: {}",
        request.getUri(),
        responseType.getSimpleName());
    return jbhHttpClientAdapter.executeForObject(request, responseType);
  }

  // Typed POST request methods

  /**
   * Performs a POST request with JSON body and deserializes the response body to the specified
   * type.
   *
   * @param <T> the type to deserialize the response body to
   * @param path the path to append to the base URL
   * @param jsonBody the JSON request body
   * @param responseType the class of the response type
   * @return the HTTP response with deserialized body
   * @throws HttpClientException if the request fails
   */
  @Override
  public <T> JbhHttpResponse postForObject(String path, String jsonBody, Class<T> responseType) {
    return postForObject(path, jsonBody, defaultJbhHttpHeaders, responseType);
  }

  /**
   * Performs a POST request with JSON body and custom headers, deserializing the response body to
   * the specified type.
   *
   * @param <T> the type to deserialize the response body to
   * @param path the path to append to the base URL
   * @param jsonBody the JSON request body
   * @param headers additional headers to include in the request
   * @param responseType the class of the response type
   * @return the HTTP response with deserialized body
   * @throws HttpClientException if the request fails
   */
  @Override
  public <T> JbhHttpResponse postForObject(
      String path, String jsonBody, JbhHttpHeaders headers, Class<T> responseType) {
    JbhHttpRequest request =
        JbhHttpRequest.post(buildUrl(path))
            .jsonBody(jsonBody)
            .headers(headers)
            .timeout(defaultTimeout)
            .build();

    log.debug(
        "Executing typed POST request to: {} for type: {}",
        request.getUri(),
        responseType.getSimpleName());
    return jbhHttpClientAdapter.executeForObject(request, responseType);
  }

  // Async methods

  /**
   * Performs an asynchronous GET request to the specified path.
   *
   * @param path the path to append to the base URL
   * @return a CompletableFuture that will complete with the HTTP response
   */
  @Override
  public CompletableFuture<JbhHttpResponse> getAsync(String path) {
    return getAsync(path, defaultJbhHttpHeaders);
  }

  /**
   * Performs an asynchronous GET request to the specified path with custom headers.
   *
   * @param path the path to append to the base URL
   * @param headers additional headers to include in the request
   * @return a CompletableFuture that will complete with the HTTP response
   */
  @Override
  public CompletableFuture<JbhHttpResponse> getAsync(String path, JbhHttpHeaders headers) {
    JbhHttpRequest request =
        JbhHttpRequest.get(buildUrl(path)).headers(headers).timeout(defaultTimeout).build();

    log.debug("Executing async GET request to: {}", request.getUri());
    return jbhHttpClientAdapter.executeAsync(request);
  }

  /**
   * Performs an asynchronous POST request to the specified path with JSON body.
   *
   * @param path the path to append to the base URL
   * @param jsonBody the JSON request body
   * @return a CompletableFuture that will complete with the HTTP response
   */
  @Override
  public CompletableFuture<JbhHttpResponse> postAsync(String path, String jsonBody) {
    return postAsync(path, jsonBody, defaultJbhHttpHeaders);
  }

  /**
   * Performs an asynchronous POST request to the specified path with JSON body and custom headers.
   *
   * @param path the path to append to the base URL
   * @param jsonBody the JSON request body
   * @param headers additional headers to include in the request
   * @return a CompletableFuture that will complete with the HTTP response
   */
  @Override
  public CompletableFuture<JbhHttpResponse> postAsync(
      String path, String jsonBody, JbhHttpHeaders headers) {
    JbhHttpRequest request =
        JbhHttpRequest.post(buildUrl(path))
            .jsonBody(jsonBody)
            .headers(headers)
            .timeout(defaultTimeout)
            .build();

    log.debug("Executing async POST request to: {}", request.getUri());
    return jbhHttpClientAdapter.executeAsync(request);
  }

  // Utility methods

  /**
   * Executes a custom HTTP request. This method provides full control over the request.
   *
   * @param request the HTTP request to execute
   * @return the HTTP response
   * @throws HttpClientException if the request fails
   */
  @Override
  public JbhHttpResponse execute(JbhHttpRequest request) {
    log.debug("Executing custom request: {} {}", request.getMethod(), request.getUri());
    return jbhHttpClientAdapter.execute(request);
  }

  /**
   * Executes a custom HTTP request asynchronously.
   *
   * @param request the HTTP request to execute
   * @return a CompletableFuture that will complete with the HTTP response
   */
  @Override
  public CompletableFuture<JbhHttpResponse> executeAsync(JbhHttpRequest request) {
    log.debug("Executing async custom request: {} {}", request.getMethod(), request.getUri());
    return jbhHttpClientAdapter.executeAsync(request);
  }

  /**
   * Executes a custom HTTP request and deserializes the response body to the specified type.
   *
   * @param <T> the type to deserialize the response body to
   * @param request the HTTP request to execute
   * @param responseType the class of the response type
   * @return the HTTP response with deserialized body
   * @throws HttpClientException if the request fails
   */
  @Override
  public <T> JbhHttpResponse executeForObject(JbhHttpRequest request, Class<T> responseType) {
    log.debug(
        "Executing typed custom request: {} {} for type: {}",
        request.getMethod(),
        request.getUri(),
        responseType.getSimpleName());
    return jbhHttpClientAdapter.executeForObject(request, responseType);
  }

  /**
   * Creates a request builder for the specified method and path. This provides a fluent API for
   * building complex requests.
   *
   * @param method the HTTP method
   * @param path the path to append to the base URL
   * @return a request builder
   */
  @Override
  public JbhHttpRequest.Builder request(JbhHttpMethod method, String path) {
    return JbhHttpRequest.builder().method(method).uri(buildUrl(path)).timeout(defaultTimeout);
  }

  /**
   * Gets the base URL configured for this client.
   *
   * @return the base URL
   */
  @Override
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Gets the name of the HTTP client adapter being used.
   *
   * @return the adapter name
   */
  @Override
  public String getClientAdapterName() {
    return jbhHttpClientAdapter.getAdapterName();
  }

  /**
   * Checks if the underlying HTTP client supports HTTP/2.
   *
   * @return true if HTTP/2 is supported
   */
  @Override
  public boolean supportsHttp2() {
    return jbhHttpClientAdapter.supportsHttp2();
  }

  @Override
  public void close() {
    log.info("Closing gateway client");
    try {
      jbhHttpClientAdapter.close();
    } catch (Exception e) {
      log.warn("Error closing HTTP client adapter: {}", e.getMessage(), e);
    }

    try {
      jbhHttpClientFactory.shutdown();
    } catch (Exception e) {
      log.warn("Error shutting down HTTP client factory: {}", e.getMessage(), e);
    }
  }

  /** Validates and normalizes the base URL. */
  private String validateAndNormalizeBaseUrl(String baseUrl) {
    Objects.requireNonNull(baseUrl, "Base URL cannot be null");

    if (baseUrl.isBlank()) {
      throw new IllegalArgumentException("Base URL cannot be blank");
    }

    try {
      URI uri = URI.create(baseUrl);
      if (uri.getScheme() == null
          || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
        throw new IllegalArgumentException("Base URL must have http or https scheme");
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid base URL format: " + baseUrl, e);
    }

    // Remove trailing slash for consistency
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  /** Builds a complete URL by combining the base URL with the given path. */
  private String buildUrl(String inputPath) {
    if (inputPath == null) {
      return baseUrl;
    }

    // Ensure path starts with /
    String normalizedPath = inputPath.startsWith("/") ? inputPath : "/" + inputPath;
    return baseUrl + normalizedPath;
  }
}
