package com.jbh.gateway.internal.core.http.adapter;

import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.config.JbhRetryConfig;
import com.jbh.gateway.internal.core.mappers.JacksonJsonUtil;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.exception.HttpClientException;
import com.jbh.gateway.internal.core.http.exception.HttpConnectionException;
import com.jbh.gateway.internal.core.http.exception.HttpTimeoutException;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import com.jbh.gateway.client.JbhHttpResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client adapter implementation using Java's native HTTP client (Java 11+). Provides
 * high-performance HTTP communication with full HTTP/2 support, connection pooling, and
 * comprehensive error handling.
 */
public class NativeHttpClientAdapter implements JbhHttpClientAdapter {

  private static final Logger log = LoggerFactory.getLogger(NativeHttpClientAdapter.class);

  private final HttpClient httpClient;
  private final JbhHttpClientConfig config;

  public NativeHttpClientAdapter(JbhHttpClientConfig config) {
    this.config = config;
    this.httpClient = createHttpClient(config);
    log.info("Initialized Native HTTP Client adapter with config: {}", config);
  }

  public static NativeHttpClientAdapter create() {
    return new NativeHttpClientAdapter(JbhHttpClientConfig.defaultConfig());
  }

  public static NativeHttpClientAdapter create(JbhHttpClientConfig config) {
    return new NativeHttpClientAdapter(config);
  }

  @Override
  public JbhHttpResponse execute(JbhHttpRequest request) {

    log.debug("Executing synchronous request: {} {}", request.getMethod(), request.getUri());

    try {
      return executeWithRetry(request, config.getRetryConfig());
    } catch (Exception e) {
      throw mapException(e, request);
    }
  }

  @Override
  public CompletableFuture<JbhHttpResponse> executeAsync(JbhHttpRequest request) {

    log.debug("Executing asynchronous request: {} {}", request.getMethod(), request.getUri());

    return executeAsyncWithRetry(request, config.getRetryConfig())
        .exceptionally(
            throwable -> {
              throw mapException(throwable, request);
            });
  }

  @Override
  public String getAdapterName() {
    return "native";
  }

  @Override
  public boolean supportsHttp2() {
    return true;
  }

  @Override
  public <T> JbhHttpResponse executeForObject(JbhHttpRequest request, Class<T> responseType) {
    log.debug("Executing synchronous typed request: {} {} -> {}", request.getMethod(), request.getUri(), responseType.getSimpleName());
    
    try {
      return executeWithRetryForObject(request, responseType, config.getRetryConfig());
    } catch (Exception e) {
      throw mapException(e, request);
    }
  }

  @Override
  public <T> CompletableFuture<JbhHttpResponse> executeAsyncForObject(JbhHttpRequest request, Class<T> responseType) {
    log.debug("Executing asynchronous typed request: {} {} -> {}", request.getMethod(), request.getUri(), responseType.getSimpleName());
    
    return executeAsyncWithRetryForObject(request, responseType, config.getRetryConfig())
        .exceptionally(throwable -> {
          throw mapException(throwable, request);
        });
  }

  @Override
  public void close() {
    log.debug("Closing Native HTTP Client adapter");
    // Native HTTP client doesn't require explicit cleanup
    // Connection pools are managed automatically by the JVM
  }

  private <T> JbhHttpResponse executeWithRetryForObject(JbhHttpRequest request, Class<T> responseType, JbhRetryConfig retryConfig) {
    Exception lastException = null;

    for (int attempt = 1; attempt <= retryConfig.getMaxAttempts(); attempt++) {
      try {
        if (attempt > 1) {
          Duration delay = retryConfig.calculateDelay(attempt);
          log.debug(
              "Retrying typed request (attempt {}/{}) after {} ms delay",
              attempt,
              retryConfig.getMaxAttempts(),
              delay.toMillis());
          Thread.sleep(delay.toMillis());
        }

        HttpRequest nativeRequest = convertRequest(request);
        HttpResponse<String> nativeResponse =
            httpClient.send(nativeRequest, HttpResponse.BodyHandlers.ofString());

        JbhHttpResponse response = convertResponseWithType(nativeResponse, responseType);

        // Check if we should retry based on status code
        if (attempt < retryConfig.getMaxAttempts()
            && retryConfig.shouldRetryForStatusCode(response.getStatusCode())) {
          log.debug(
              "Received retryable status code {} for typed attempt {}/{}",
              response.getStatusCode(),
              attempt,
              retryConfig.getMaxAttempts());
          continue;
        }

        log.debug(
            "Typed request completed successfully on attempt {}/{} with status {}",
            attempt,
            retryConfig.getMaxAttempts(),
            response.getStatusCode());
        return response;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new HttpClientException("Typed request was interrupted", e);
      } catch (Exception e) {
        lastException = e;

        if (attempt >= retryConfig.getMaxAttempts() || !retryConfig.shouldRetryForException(e)) {
          break;
        }

        log.debug(
            "Typed request failed on attempt {}/{}, will retry: {}",
            attempt,
            retryConfig.getMaxAttempts(),
            e.getMessage());
      }
    }

    throw mapException(lastException, request);
  }

  private <T> CompletableFuture<JbhHttpResponse> executeAsyncWithRetryForObject(
      JbhHttpRequest request, Class<T> responseType, JbhRetryConfig retryConfig) {
    return executeAsyncWithRetryInternalForObject(request, responseType, retryConfig, 1);
  }

  private <T> CompletableFuture<JbhHttpResponse> executeAsyncWithRetryInternalForObject(
      JbhHttpRequest request, Class<T> responseType, JbhRetryConfig retryConfig, int attempt) {

    try {
      HttpRequest nativeRequest = convertRequest(request);

      return httpClient
          .sendAsync(nativeRequest, HttpResponse.BodyHandlers.ofString())
          .thenApply(response -> convertResponseWithType(response, responseType))
          .thenCompose(
              response -> {
                // Check if we should retry based on status code
                if (attempt < retryConfig.getMaxAttempts()
                    && retryConfig.shouldRetryForStatusCode(response.getStatusCode())) {

                  Duration delay = retryConfig.calculateDelay(attempt + 1);
                  log.debug(
                      "Received retryable status code {} for async typed attempt {}/{}, retrying after {} ms",
                      response.getStatusCode(),
                      attempt,
                      retryConfig.getMaxAttempts(),
                      delay.toMillis());

                  CompletableFuture<Void> delayFuture = new CompletableFuture<>();
                  java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                      .schedule(
                          () -> delayFuture.complete(null),
                          delay.toNanos(),
                          java.util.concurrent.TimeUnit.NANOSECONDS);
                  return delayFuture.thenCompose(
                      v -> executeAsyncWithRetryInternalForObject(request, responseType, retryConfig, attempt + 1));
                }

                return CompletableFuture.completedFuture(response);
              })
          .exceptionally(
              throwable -> {
                if (attempt < retryConfig.getMaxAttempts()
                    && retryConfig.shouldRetryForException(throwable)) {

                  Duration delay = retryConfig.calculateDelay(attempt + 1);
                  log.debug(
                      "Async typed request failed on attempt {}/{}, retrying after {} ms: {}",
                      attempt,
                      retryConfig.getMaxAttempts(),
                      delay.toMillis(),
                      throwable.getMessage());

                  CompletableFuture<Void> delayFuture = new CompletableFuture<>();
                  java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                      .schedule(
                          () -> delayFuture.complete(null),
                          delay.toNanos(),
                          java.util.concurrent.TimeUnit.NANOSECONDS);
                  return delayFuture
                      .thenCompose(
                          v -> executeAsyncWithRetryInternalForObject(request, responseType, retryConfig, attempt + 1))
                      .join();
                }

                throw mapException(throwable, request);
              });

    } catch (Exception e) {
      return CompletableFuture.failedFuture(mapException(e, request));
    }
  }

  private HttpClient createHttpClient(JbhHttpClientConfig config) {
    HttpClient.Builder builder =
        HttpClient.newBuilder()
            .connectTimeout(config.getConnectTimeout())
            .followRedirects(
                config.isFollowRedirects()
                    ? HttpClient.Redirect.NORMAL
                    : HttpClient.Redirect.NEVER);

    if (config.isEnableHttp2()) {
      builder.version(HttpClient.Version.HTTP_2);
    } else {
      builder.version(HttpClient.Version.HTTP_1_1);
    }

    // Use virtual threads executor if available (Java 21+)
    try {
      // Use reflection to check for virtual thread support
      Class<?> threadClass = Thread.class;
      var method = threadClass.getMethod("ofVirtual");
      var virtualThreadBuilder = method.invoke(null);
      var factory =
          virtualThreadBuilder.getClass().getMethod("factory").invoke(virtualThreadBuilder);

      Executor virtualThreadExecutor =
          (Executor)
              factory
                  .getClass()
                  .getMethod("newThread", Runnable.class)
                  .invoke(factory, (Runnable) () -> {});

      // This is a simplified approach - in real implementation you'd create a proper executor
      log.debug("Virtual threads are available but not configured in this example");
    } catch (Exception e) {
      log.debug("Virtual threads not available, using default executor");
    }

    return builder.build();
  }

  private JbhHttpResponse executeWithRetry(JbhHttpRequest request, JbhRetryConfig retryConfig) {

    Exception lastException = null;

    for (int attempt = 1; attempt <= retryConfig.getMaxAttempts(); attempt++) {
      try {
        if (attempt > 1) {
          Duration delay = retryConfig.calculateDelay(attempt);
          log.debug(
              "Retrying request (attempt {}/{}) after {} ms delay",
              attempt,
              retryConfig.getMaxAttempts(),
              delay.toMillis());
          Thread.sleep(delay.toMillis());
        }

        HttpRequest nativeRequest = convertRequest(request);
        HttpResponse<String> nativeResponse =
            httpClient.send(nativeRequest, HttpResponse.BodyHandlers.ofString());

        JbhHttpResponse response = convertResponse(nativeResponse);

        // Check if we should retry based on status code
        if (attempt < retryConfig.getMaxAttempts()
            && retryConfig.shouldRetryForStatusCode(response.getStatusCode())) {
          log.debug(
              "Received retryable status code {} for attempt {}/{}",
              response.getStatusCode(),
              attempt,
              retryConfig.getMaxAttempts());
          continue;
        }

        log.debug(
            "Request completed successfully on attempt {}/{} with status {}",
            attempt,
            retryConfig.getMaxAttempts(),
            response.getStatusCode());
        return response;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new HttpClientException("Request was interrupted", e);
      } catch (Exception e) {
        lastException = e;

        if (attempt >= retryConfig.getMaxAttempts() || !retryConfig.shouldRetryForException(e)) {
          break;
        }

        log.debug(
            "Request failed on attempt {}/{}, will retry: {}",
            attempt,
            retryConfig.getMaxAttempts(),
            e.getMessage());
      }
    }

    throw mapException(lastException, request);
  }

  private CompletableFuture<JbhHttpResponse> executeAsyncWithRetry(
      JbhHttpRequest request, JbhRetryConfig retryConfig) {

    return executeAsyncWithRetryInternal(request, retryConfig, 1);
  }

  private CompletableFuture<JbhHttpResponse> executeAsyncWithRetryInternal(
      JbhHttpRequest request, JbhRetryConfig retryConfig, int attempt) {

    try {
      HttpRequest nativeRequest = convertRequest(request);

      return httpClient
          .sendAsync(nativeRequest, HttpResponse.BodyHandlers.ofString())
          .thenApply(this::convertResponse)
          .thenCompose(
              response -> {
                // Check if we should retry based on status code
                if (attempt < retryConfig.getMaxAttempts()
                    && retryConfig.shouldRetryForStatusCode(response.getStatusCode())) {

                  Duration delay = retryConfig.calculateDelay(attempt + 1);
                  log.debug(
                      "Received retryable status code {} for async attempt {}/{}, retrying after {} ms",
                      response.getStatusCode(),
                      attempt,
                      retryConfig.getMaxAttempts(),
                      delay.toMillis());

                  CompletableFuture<Void> delayFuture = new CompletableFuture<>();
                  java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                      .schedule(
                          () -> delayFuture.complete(null),
                          delay.toNanos(),
                          java.util.concurrent.TimeUnit.NANOSECONDS);
                  return delayFuture.thenCompose(
                      v -> executeAsyncWithRetryInternal(request, retryConfig, attempt + 1));
                }

                return CompletableFuture.completedFuture(response);
              })
          .exceptionally(
              throwable -> {
                if (attempt < retryConfig.getMaxAttempts()
                    && retryConfig.shouldRetryForException(throwable)) {

                  Duration delay = retryConfig.calculateDelay(attempt + 1);
                  log.debug(
                      "Async request failed on attempt {}/{}, retrying after {} ms: {}",
                      attempt,
                      retryConfig.getMaxAttempts(),
                      delay.toMillis(),
                      throwable.getMessage());

                  CompletableFuture<Void> delayFuture = new CompletableFuture<>();
                  java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                      .schedule(
                          () -> delayFuture.complete(null),
                          delay.toNanos(),
                          java.util.concurrent.TimeUnit.NANOSECONDS);
                  return delayFuture
                      .thenCompose(
                          v -> executeAsyncWithRetryInternal(request, retryConfig, attempt + 1))
                      .join();
                }

                throw mapException(throwable, request);
              });

    } catch (Exception e) {
      return CompletableFuture.failedFuture(mapException(e, request));
    }
  }

  private HttpRequest convertRequest(JbhHttpRequest request) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder().uri(request.getUri()).timeout(request.getTimeout());

    // Add headers
    request
        .getHeaders()
        .asMap()
        .forEach((name, values) -> values.forEach(value -> builder.header(name, value)));

    // Set method and body
    HttpRequest.BodyPublisher bodyPublisher =
        request
            .getBody()
            .map(HttpRequest.BodyPublishers::ofString)
            .orElse(HttpRequest.BodyPublishers.noBody());

    switch (request.getMethod()) {
      case GET -> builder.GET();
      case POST -> builder.POST(bodyPublisher);
      case PUT -> builder.PUT(bodyPublisher);
      case DELETE -> builder.DELETE();
      case PATCH -> builder.method("PATCH", bodyPublisher);
      case HEAD -> builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
      case OPTIONS -> builder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
      default ->
          throw new IllegalArgumentException("Unsupported HTTP method: " + request.getMethod());
    }

    return builder.build();
  }

  private JbhHttpResponse convertResponse(HttpResponse<String> nativeResponse) {
    JbhHttpHeaders.Builder headersBuilder = JbhHttpHeaders.builder();
    nativeResponse
        .headers()
        .map()
        .forEach((name, values) -> values.forEach(value -> headersBuilder.add(name, value)));

    return JbhHttpResponse.of(
        nativeResponse.statusCode(), headersBuilder.build(), nativeResponse.body());
  }

  private <T> JbhHttpResponse convertResponseWithType(HttpResponse<String> nativeResponse, Class<T> responseType) {
    JbhHttpHeaders.Builder headersBuilder = JbhHttpHeaders.builder();
    nativeResponse
        .headers()
        .map()
        .forEach((name, values) -> values.forEach(value -> headersBuilder.add(name, value)));

    String responseBody = nativeResponse.body();
    T typedBody = null;

    // Only attempt JSON deserialization for successful responses with a body
    if (nativeResponse.statusCode() >= 200 && nativeResponse.statusCode() < 300 && responseBody != null && !responseBody.isEmpty()) {
      try {
        typedBody = JacksonJsonUtil.fromJson(responseBody, responseType);
        log.debug("Successfully deserialized response body to {}", responseType.getSimpleName());
      } catch (Exception e) {
        log.warn("Failed to deserialize response body to {}: {}", responseType.getSimpleName(), e.getMessage());
        // Continue with null typedBody - the raw string body will still be available
      }
    }

    return JbhHttpResponse.ofTyped(
        nativeResponse.statusCode(), headersBuilder.build(), responseBody, typedBody);
  }

  private RuntimeException mapException(Throwable throwable, JbhHttpRequest request) {
    if (throwable instanceof RuntimeException runtimeException) {
      return runtimeException;
    }

    String requestInfo = String.format("%s %s", request.getMethod(), request.getUri());

    if (throwable instanceof java.net.http.HttpTimeoutException) {
      return new HttpTimeoutException(
          "Request timeout for " + requestInfo, request.getTimeout().toMillis(), throwable);
    } else if (throwable instanceof ConnectException) {
      return new HttpConnectionException(
          "Connection failed for " + requestInfo, request.getUri().getHost(), throwable);
    } else if (throwable instanceof IOException) {
      return new HttpClientException(
          "IO error during request to " + requestInfo, "IO_ERROR", null, throwable);
    } else {
      return new HttpClientException(
          "Unexpected error during request to " + requestInfo, "UNKNOWN_ERROR", null, throwable);
    }
  }
}
