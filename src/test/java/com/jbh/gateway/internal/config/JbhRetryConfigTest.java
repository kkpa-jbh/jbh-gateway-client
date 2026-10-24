package com.jbh.gateway.internal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.gateway.internal.core.http.model.JbhHttpMethod;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JbhRetryConfigTest {

  private final JbhRetryConfig config = JbhRetryConfig.defaultConfig();

  @Test
  void firstRetryWaitsInitialDelay() {
    assertEquals(Duration.ofMillis(500), config.calculateDelay(2));
    assertEquals(Duration.ofMillis(1000), config.calculateDelay(3));
  }

  @Test
  void delayStopsAtMaxDelay() {
    JbhRetryConfig capped =
        JbhRetryConfig.builder()
            .initialDelay(Duration.ofSeconds(1))
            .maxDelay(Duration.ofSeconds(3))
            .build();

    assertEquals(Duration.ofSeconds(3), capped.calculateDelay(5));
  }

  @Test
  void retryableStatusIsRetriedOnlyForIdempotentMethods() {
    assertTrue(config.shouldRetryForStatusCode(JbhHttpMethod.GET, 503));
    assertTrue(config.shouldRetryForStatusCode(JbhHttpMethod.PUT, 502));
    assertFalse(config.shouldRetryForStatusCode(JbhHttpMethod.POST, 504));
    assertFalse(config.shouldRetryForStatusCode(JbhHttpMethod.PATCH, 503));
    assertFalse(config.shouldRetryForStatusCode(JbhHttpMethod.GET, 500));
  }

  @Test
  void failedConnectionIsRetriedForEveryMethod() {
    assertTrue(config.shouldRetryForException(JbhHttpMethod.POST, new ConnectException()));
    assertTrue(config.shouldRetryForException(JbhHttpMethod.GET, new ConnectException()));
  }

  @Test
  void timeoutIsRetriedOnlyForIdempotentMethods() {
    assertTrue(config.shouldRetryForException(JbhHttpMethod.GET, new HttpTimeoutException("t")));
    assertFalse(config.shouldRetryForException(JbhHttpMethod.POST, new HttpTimeoutException("t")));
    assertFalse(config.shouldRetryForException(JbhHttpMethod.POST, new IOException("reset")));
  }
}
