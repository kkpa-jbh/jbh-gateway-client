package com.jbh.gateway.internal.config;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Settings of the circuit breaker around each domain client.
 *
 * <p>The breaker counts the last {@code slidingWindowSize} calls. When at least {@code
 * minimumNumberOfCalls} were made and {@code failureRateThreshold} percent of them failed, it
 * opens. While open, calls fail at once. After {@code waitDurationInOpenState} it lets {@code
 * permittedCallsInHalfOpenState} test calls through, then closes or opens again.
 *
 * <p>A failure is a call with no HTTP response (connection error, timeout), or a response with a
 * status in {@code failureStatusCodes}. A 4xx is the caller's problem, not a down service, so it
 * never counts.
 *
 * <p>The state lives in memory, per client instance. Each service instance has its own breaker.
 * That is fine: the breaker only protects the caller, it guards no shared data.
 */
public final class JbhCircuitBreakerConfig {

  private final float failureRateThreshold;
  private final int slidingWindowSize;
  private final int minimumNumberOfCalls;
  private final Duration waitDurationInOpenState;
  private final int permittedCallsInHalfOpenState;
  private final Set<Integer> failureStatusCodes;

  private JbhCircuitBreakerConfig(Builder builder) {
    this.failureRateThreshold = builder.failureRateThreshold;
    this.slidingWindowSize = builder.slidingWindowSize;
    this.minimumNumberOfCalls = builder.minimumNumberOfCalls;
    this.waitDurationInOpenState = builder.waitDurationInOpenState;
    this.permittedCallsInHalfOpenState = builder.permittedCallsInHalfOpenState;
    this.failureStatusCodes = Set.copyOf(builder.failureStatusCodes);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static JbhCircuitBreakerConfig defaultConfig() {
    return builder().build();
  }

  public float getFailureRateThreshold() {
    return failureRateThreshold;
  }

  public int getSlidingWindowSize() {
    return slidingWindowSize;
  }

  public int getMinimumNumberOfCalls() {
    return minimumNumberOfCalls;
  }

  public Duration getWaitDurationInOpenState() {
    return waitDurationInOpenState;
  }

  public int getPermittedCallsInHalfOpenState() {
    return permittedCallsInHalfOpenState;
  }

  /** True when a response with this status means the service behind the gateway is not healthy. */
  public boolean isFailureStatusCode(int statusCode) {
    return failureStatusCodes.contains(statusCode);
  }

  @Override
  public String toString() {
    return "CircuitBreakerConfig{"
        + "failureRateThreshold="
        + failureRateThreshold
        + ", slidingWindowSize="
        + slidingWindowSize
        + ", minimumNumberOfCalls="
        + minimumNumberOfCalls
        + ", waitDurationInOpenState="
        + waitDurationInOpenState
        + ", permittedCallsInHalfOpenState="
        + permittedCallsInHalfOpenState
        + ", failureStatusCodes="
        + failureStatusCodes
        + '}';
  }

  public static class Builder {
    private float failureRateThreshold = 50f;
    private int slidingWindowSize = 20;
    private int minimumNumberOfCalls = 10;
    private Duration waitDurationInOpenState = Duration.ofSeconds(30);
    private int permittedCallsInHalfOpenState = 3;
    private Set<Integer> failureStatusCodes = Set.of(502, 503, 504);

    public Builder failureRateThreshold(float failureRateThreshold) {
      if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
        throw new IllegalArgumentException("Failure rate threshold must be in (0, 100]");
      }
      this.failureRateThreshold = failureRateThreshold;
      return this;
    }

    public Builder slidingWindowSize(int slidingWindowSize) {
      if (slidingWindowSize < 1) {
        throw new IllegalArgumentException("Sliding window size must be at least 1");
      }
      this.slidingWindowSize = slidingWindowSize;
      return this;
    }

    public Builder minimumNumberOfCalls(int minimumNumberOfCalls) {
      if (minimumNumberOfCalls < 1) {
        throw new IllegalArgumentException("Minimum number of calls must be at least 1");
      }
      this.minimumNumberOfCalls = minimumNumberOfCalls;
      return this;
    }

    public Builder waitDurationInOpenState(Duration waitDurationInOpenState) {
      this.waitDurationInOpenState =
          Objects.requireNonNull(waitDurationInOpenState, "Wait duration cannot be null");
      return this;
    }

    public Builder permittedCallsInHalfOpenState(int permittedCallsInHalfOpenState) {
      if (permittedCallsInHalfOpenState < 1) {
        throw new IllegalArgumentException("Permitted calls in half-open state must be at least 1");
      }
      this.permittedCallsInHalfOpenState = permittedCallsInHalfOpenState;
      return this;
    }

    public Builder failureStatusCodes(Set<Integer> failureStatusCodes) {
      this.failureStatusCodes =
          Objects.requireNonNull(failureStatusCodes, "Failure status codes cannot be null");
      return this;
    }

    public JbhCircuitBreakerConfig build() {
      return new JbhCircuitBreakerConfig(this);
    }
  }
}
