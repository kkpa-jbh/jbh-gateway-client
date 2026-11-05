package com.jbh.gateway.internal.core.resilience;

import java.io.IOException;

/**
 * The circuit breaker is open, so the request was not sent. It is an {@link IOException}, like
 * every other "no HTTP response" case, so {@code BaseApiGatewayClient} maps it in one place.
 */
public class CircuitOpenException extends IOException {

  private static final long serialVersionUID = 1L;

  public CircuitOpenException(String circuitBreakerName, Throwable cause) {
    super("Circuit breaker '" + circuitBreakerName + "' is open, the request was not sent", cause);
  }
}
