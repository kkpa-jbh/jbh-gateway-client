package com.jbh.gateway.internal.core.resilience;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.config.JbhCircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a Resilience4j circuit breaker from a {@link JbhCircuitBreakerConfig}. It is the only
 * place that knows Resilience4j settings, metrics and state logs.
 *
 * <p>Metrics (when a {@link MeterRegistry} is given) use the standard Resilience4j names, tagged
 * with {@code name}: {@code resilience4j_circuitbreaker_state}, {@code
 * resilience4j_circuitbreaker_calls_seconds}, {@code resilience4j_circuitbreaker_failure_rate},
 * {@code resilience4j_circuitbreaker_not_permitted_calls_total}, and others.
 */
public final class JbhCircuitBreakerFactory {

  private static final Logger log = LoggerFactory.getLogger(JbhCircuitBreakerFactory.class);

  private JbhCircuitBreakerFactory() {}

  /**
   * @param name the breaker name, also the {@code name} tag of its metrics
   * @param config the thresholds and the status codes that count as failures
   * @param meterRegistry where to publish the metrics, or {@code null} for no metrics
   */
  public static CircuitBreaker create(
      String name, JbhCircuitBreakerConfig config, MeterRegistry meterRegistry) {
    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(toResilience4jConfig(config));
    if (meterRegistry != null) {
      TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meterRegistry);
    }
    CircuitBreaker circuitBreaker = registry.circuitBreaker(name);
    circuitBreaker
        .getEventPublisher()
        .onStateTransition(
            event ->
                log.warn(
                    "Circuit breaker '{}' changed state: {}",
                    event.getCircuitBreakerName(),
                    event.getStateTransition()));
    log.debug("Circuit breaker '{}' created with {}", name, config);
    return circuitBreaker;
  }

  private static CircuitBreakerConfig toResilience4jConfig(JbhCircuitBreakerConfig config) {
    return CircuitBreakerConfig.custom()
        .slidingWindowType(SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(config.getSlidingWindowSize())
        .minimumNumberOfCalls(config.getMinimumNumberOfCalls())
        .failureRateThreshold(config.getFailureRateThreshold())
        .waitDurationInOpenState(config.getWaitDurationInOpenState())
        .permittedNumberOfCallsInHalfOpenState(config.getPermittedCallsInHalfOpenState())
        .automaticTransitionFromOpenToHalfOpenEnabled(false)
        // No HTTP response is a failure. An interrupt is neither a failure nor a success.
        .recordException(IOException.class::isInstance)
        .ignoreExceptions(InterruptedException.class)
        .recordResult(
            result ->
                result instanceof JbhHttpResponse response
                    && config.isFailureStatusCode(response.getStatusCode()))
        .build();
  }
}
