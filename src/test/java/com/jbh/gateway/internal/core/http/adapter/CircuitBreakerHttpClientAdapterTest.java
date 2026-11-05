package com.jbh.gateway.internal.core.http.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.config.JbhCircuitBreakerConfig;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import com.jbh.gateway.internal.core.resilience.CircuitOpenException;
import com.jbh.gateway.internal.core.resilience.JbhCircuitBreakerFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The breaker opens on 5xx and network errors, ignores 4xx, and stops sending while open. */
class CircuitBreakerHttpClientAdapterTest {

  private static final JbhHttpRequest REQUEST =
      JbhHttpRequest.get("http://localhost:8080/jbh-api/users/find-user-id").build();

  // Opens after 4 calls if half of them failed. Stays open long enough for the test.
  private static final JbhCircuitBreakerConfig CONFIG =
      JbhCircuitBreakerConfig.builder()
          .slidingWindowSize(4)
          .minimumNumberOfCalls(4)
          .failureRateThreshold(50f)
          .waitDurationInOpenState(Duration.ofMinutes(1))
          .build();

  private static JbhHttpResponse response(int statusCode) {
    return JbhHttpResponse.of(statusCode, JbhHttpHeaders.empty(), "");
  }

  private static CircuitBreaker breaker() {
    return JbhCircuitBreakerFactory.create("test", CONFIG, null);
  }

  @Test
  void opensAfterTooMany5xxAndStopsSending() throws Exception {
    AtomicInteger sent = new AtomicInteger();
    JbhHttpClientAdapter transport =
        request -> {
          sent.incrementAndGet();
          return response(503);
        };
    CircuitBreaker circuitBreaker = breaker();
    CircuitBreakerHttpClientAdapter adapter =
        new CircuitBreakerHttpClientAdapter(transport, circuitBreaker);

    for (int i = 0; i < 4; i++) {
      assertEquals(503, adapter.execute(REQUEST).getStatusCode());
    }

    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    assertThrows(CircuitOpenException.class, () -> adapter.execute(REQUEST));
    assertEquals(4, sent.get());
  }

  @Test
  void networkErrorsCountAsFailures() {
    JbhHttpClientAdapter transport =
        request -> {
          throw new ConnectException("refused");
        };
    CircuitBreaker circuitBreaker = breaker();
    CircuitBreakerHttpClientAdapter adapter =
        new CircuitBreakerHttpClientAdapter(transport, circuitBreaker);

    for (int i = 0; i < 4; i++) {
      assertThrows(ConnectException.class, () -> adapter.execute(REQUEST));
    }

    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
  }

  @Test
  void clientErrorsDoNotOpenTheBreaker() throws Exception {
    JbhHttpClientAdapter transport = request -> response(404);
    CircuitBreaker circuitBreaker = breaker();
    CircuitBreakerHttpClientAdapter adapter =
        new CircuitBreakerHttpClientAdapter(transport, circuitBreaker);

    for (int i = 0; i < 10; i++) {
      assertEquals(404, adapter.execute(REQUEST).getStatusCode());
    }

    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
  }

  @Test
  void interruptsDoNotCountAsFailures() {
    JbhHttpClientAdapter transport =
        request -> {
          throw new InterruptedException("stop");
        };
    CircuitBreaker circuitBreaker = breaker();
    CircuitBreakerHttpClientAdapter adapter =
        new CircuitBreakerHttpClientAdapter(transport, circuitBreaker);

    for (int i = 0; i < 4; i++) {
      assertThrows(InterruptedException.class, () -> adapter.execute(REQUEST));
    }

    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    assertEquals(0, circuitBreaker.getMetrics().getNumberOfFailedCalls());
  }
}
