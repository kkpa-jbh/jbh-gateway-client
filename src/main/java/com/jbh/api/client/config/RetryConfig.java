package com.jbh.api.client.config;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Configuration for retry behavior in HTTP client adapters.
 * Defines retry policies including maximum attempts, backoff strategies,
 * and conditions for when retries should be attempted.
 */
public final class RetryConfig {

    private final int maxAttempts;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final Set<Integer> retryableStatusCodes;
    private final Predicate<Throwable> retryableExceptions;
    private final boolean retryOnTimeout;
    private final boolean retryOnConnectionFailure;

    private RetryConfig(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.retryableStatusCodes = Set.copyOf(builder.retryableStatusCodes);
        this.retryableExceptions = builder.retryableExceptions;
        this.retryOnTimeout = builder.retryOnTimeout;
        this.retryOnConnectionFailure = builder.retryOnConnectionFailure;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RetryConfig defaultConfig() {
        return builder().build();
    }

    public static RetryConfig noRetries() {
        return builder().maxAttempts(1).build();
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public Set<Integer> getRetryableStatusCodes() {
        return retryableStatusCodes;
    }

    public Predicate<Throwable> getRetryableExceptions() {
        return retryableExceptions;
    }

    public boolean isRetryOnTimeout() {
        return retryOnTimeout;
    }

    public boolean isRetryOnConnectionFailure() {
        return retryOnConnectionFailure;
    }

    public Duration calculateDelay(int attemptNumber) {
        if (attemptNumber <= 1) {
            return initialDelay;
        }
        
        long delayMillis = (long) (initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptNumber - 1));
        Duration calculatedDelay = Duration.ofMillis(delayMillis);
        
        return calculatedDelay.compareTo(maxDelay) > 0 ? maxDelay : calculatedDelay;
    }

    public boolean shouldRetry(int attemptNumber) {
        return attemptNumber < maxAttempts;
    }

    public boolean shouldRetryForStatusCode(int statusCode) {
        return retryableStatusCodes.contains(statusCode);
    }

    public boolean shouldRetryForException(Throwable exception) {
        return retryableExceptions.test(exception);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetryConfig that = (RetryConfig) o;
        return maxAttempts == that.maxAttempts &&
                Double.compare(that.backoffMultiplier, backoffMultiplier) == 0 &&
                retryOnTimeout == that.retryOnTimeout &&
                retryOnConnectionFailure == that.retryOnConnectionFailure &&
                Objects.equals(initialDelay, that.initialDelay) &&
                Objects.equals(maxDelay, that.maxDelay) &&
                Objects.equals(retryableStatusCodes, that.retryableStatusCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxAttempts, initialDelay, maxDelay, backoffMultiplier,
                retryableStatusCodes, retryOnTimeout, retryOnConnectionFailure);
    }

    @Override
    public String toString() {
        return "RetryConfig{" +
                "maxAttempts=" + maxAttempts +
                ", initialDelay=" + initialDelay +
                ", maxDelay=" + maxDelay +
                ", backoffMultiplier=" + backoffMultiplier +
                ", retryableStatusCodes=" + retryableStatusCodes +
                ", retryOnTimeout=" + retryOnTimeout +
                ", retryOnConnectionFailure=" + retryOnConnectionFailure +
                '}';
    }

    public static class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(500);
        private Duration maxDelay = Duration.ofSeconds(10);
        private double backoffMultiplier = 2.0;
        private Set<Integer> retryableStatusCodes = Set.of(502, 503, 504, 429);
        private Predicate<Throwable> retryableExceptions = throwable -> 
                throwable instanceof java.net.ConnectException ||
                throwable instanceof java.net.SocketTimeoutException ||
                throwable instanceof java.io.IOException;
        private boolean retryOnTimeout = true;
        private boolean retryOnConnectionFailure = true;

        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("Max attempts must be at least 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = Objects.requireNonNull(initialDelay, "Initial delay cannot be null");
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = Objects.requireNonNull(maxDelay, "Max delay cannot be null");
            return this;
        }

        public Builder backoffMultiplier(double backoffMultiplier) {
            if (backoffMultiplier <= 0) {
                throw new IllegalArgumentException("Backoff multiplier must be positive");
            }
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }

        public Builder retryableStatusCodes(Set<Integer> retryableStatusCodes) {
            this.retryableStatusCodes = Objects.requireNonNull(retryableStatusCodes, "Retryable status codes cannot be null");
            return this;
        }

        public Builder retryableExceptions(Predicate<Throwable> retryableExceptions) {
            this.retryableExceptions = Objects.requireNonNull(retryableExceptions, "Retryable exceptions predicate cannot be null");
            return this;
        }

        public Builder retryOnTimeout(boolean retryOnTimeout) {
            this.retryOnTimeout = retryOnTimeout;
            return this;
        }

        public Builder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
            this.retryOnConnectionFailure = retryOnConnectionFailure;
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }
}