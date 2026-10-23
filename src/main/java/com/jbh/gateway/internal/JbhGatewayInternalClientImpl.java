package com.jbh.gateway.internal;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import com.jbh.gateway.internal.core.mappers.JacksonJsonUtil;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds requests to the gateway (base URL, headers, timeout) and sends them through the {@link
 * JbhHttpClientAdapter}. It can also read a JSON response body as a typed object.
 */
public class JbhGatewayInternalClientImpl {

  private static final Logger log = LoggerFactory.getLogger(JbhGatewayInternalClientImpl.class);

  private final String baseUrl;
  private final JbhHttpClientAdapter jbhHttpClientAdapter;
  private final Duration defaultTimeout;
  private final JbhHttpHeaders defaultJbhHttpHeaders;

  /**
   * @param baseUrl the base URL of the gateway service
   * @param httpClientConfig the request timeout and the default headers
   * @param jbhHttpClientAdapter the transport that sends the requests
   * @throws IllegalArgumentException if the base URL is invalid
   */
  public JbhGatewayInternalClientImpl(
      String baseUrl, JbhHttpClientConfig httpClientConfig, JbhHttpClientAdapter jbhHttpClientAdapter) {
    this.baseUrl = validateAndNormalizeBaseUrl(baseUrl);
    this.jbhHttpClientAdapter = jbhHttpClientAdapter;
    this.defaultTimeout = httpClientConfig.getRequestTimeout();
    this.defaultJbhHttpHeaders = JbhHttpHeaders.empty().withAll(httpClientConfig.getCustomHeaders());

    log.info(
        "Gateway client initialized with base URL: {}, headers: {}",
        this.baseUrl,
        defaultJbhHttpHeaders);
  }

  // Only GET adds the default headers (source service). POST, PUT and DELETE never did.

  public JbhHttpResponse get(String path, JbhHttpHeaders headers)
      throws IOException, InterruptedException {
    JbhHttpRequest request =
        JbhHttpRequest.get(buildUrl(path))
            .headers(defaultJbhHttpHeaders.withAll(headers))
            .timeout(defaultTimeout)
            .build();
    return jbhHttpClientAdapter.execute(request);
  }

  /** GET that also reads a JSON body of a 2xx response as {@code responseType}. */
  public <T> JbhHttpResponse get(String path, JbhHttpHeaders headers, Class<T> responseType)
      throws IOException, InterruptedException {
    return withTypedBody(get(path, headers), responseType);
  }

  public JbhHttpResponse post(String path, String jsonBody, JbhHttpHeaders headers)
      throws IOException, InterruptedException {
    log.info("Executing POST request to: {}", path);
    JbhHttpRequest request =
        JbhHttpRequest.post(buildUrl(path))
            .jsonBody(jsonBody)
            .headers(headers)
            .timeout(defaultTimeout)
            .build();
    return jbhHttpClientAdapter.execute(request);
  }

  public JbhHttpResponse put(String path, String jsonBody, JbhHttpHeaders headers)
      throws IOException, InterruptedException {
    JbhHttpRequest request =
        JbhHttpRequest.put(buildUrl(path))
            .jsonBody(jsonBody)
            .headers(headers)
            .timeout(defaultTimeout)
            .build();
    return jbhHttpClientAdapter.execute(request);
  }

  public JbhHttpResponse delete(String path, JbhHttpHeaders headers)
      throws IOException, InterruptedException {
    JbhHttpRequest request =
        JbhHttpRequest.delete(buildUrl(path)).headers(headers).timeout(defaultTimeout).build();
    return jbhHttpClientAdapter.execute(request);
  }

  /** Reads a non-empty 2xx body as {@code responseType}. If that fails, the raw body is kept. */
  private static <T> JbhHttpResponse withTypedBody(JbhHttpResponse response, Class<T> responseType) {
    Optional<String> body = response.getBody();
    if (!response.isSuccessful() || body.isEmpty() || body.get().isEmpty()) {
      return response;
    }
    try {
      T typedBody = JacksonJsonUtil.fromJson(body.get(), responseType);
      return JbhHttpResponse.ofTyped(
          response.getStatusCode(), response.getHeaders(), body.get(), typedBody);
    } catch (Exception e) {
      log.warn(
          "Failed to deserialize response body to {}: {}",
          responseType.getSimpleName(),
          e.getMessage());
      return response;
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
