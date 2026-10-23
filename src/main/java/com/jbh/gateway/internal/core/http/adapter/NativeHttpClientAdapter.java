package com.jbh.gateway.internal.core.http.adapter;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.config.JbhRetryConfig;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpMethod;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client adapter that uses Java's native HTTP client (java.net.http). It retries a request
 * as {@link JbhRetryConfig} allows.
 */
public class NativeHttpClientAdapter implements JbhHttpClientAdapter {

  private static final Logger log = LoggerFactory.getLogger(NativeHttpClientAdapter.class);

  private final HttpClient httpClient;
  private final JbhRetryConfig retryConfig;

  public NativeHttpClientAdapter(JbhHttpClientConfig config) {
    this.retryConfig = config.getRetryConfig();
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(config.getConnectTimeout())
            .followRedirects(
                config.isFollowRedirects() ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER)
            .version(config.isEnableHttp2() ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1)
            .build();
    log.info("Initialized Native HTTP Client adapter with config: {}", config);
  }

  @Override
  public JbhHttpResponse execute(JbhHttpRequest request) throws IOException, InterruptedException {
    JbhHttpMethod method = request.getMethod();
    int maxAttempts = retryConfig.getMaxAttempts();
    HttpRequest nativeRequest = convertRequest(request);

    for (int attempt = 1; ; attempt++) {
      if (attempt > 1) {
        long delayMillis = retryConfig.calculateDelay(attempt).toMillis();
        log.info(
            "Retrying request (attempt {}/{}) after {} ms delay", attempt, maxAttempts, delayMillis);
        Thread.sleep(delayMillis);
      }

      log.info("Executing request: {} {}", method, request.getUri());
      JbhHttpResponse response;
      try {
        response = convertResponse(httpClient.send(nativeRequest, HttpResponse.BodyHandlers.ofString()));
      } catch (IOException e) {
        if (attempt >= maxAttempts || !retryConfig.shouldRetryForException(method, e)) {
          throw e;
        }
        log.debug("Request failed on attempt {}/{}, will retry: {}", attempt, maxAttempts, e.toString());
        continue;
      }

      if (attempt < maxAttempts && retryConfig.shouldRetryForStatusCode(method, response.getStatusCode())) {
        log.info(
            "Received retryable status code {} for attempt {}/{}",
            response.getStatusCode(),
            attempt,
            maxAttempts);
        continue;
      }

      log.info("Request completed with status {}", response.getStatusCode());
      return response;
    }
  }

  private HttpRequest convertRequest(JbhHttpRequest request) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder().uri(request.getUri()).timeout(request.getTimeout());

    request
        .getHeaders()
        .asMap()
        .forEach((name, values) -> values.forEach(value -> builder.header(name, value)));

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
}
