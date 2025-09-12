package com.jbh.api.client.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration class for HTTP client adapters.
 * Contains common configuration options that can be applied to different HTTP client implementations.
 * Supports fluent builder pattern for easy configuration setup.
 */
public final class JbhHttpClientConfig {

    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final Duration readTimeout;
    private final int maxConnections;
    private final int maxConnectionsPerHost;
    private final boolean followRedirects;
    private final boolean enableHttp2;
    private final boolean enableCompression;
    private final Optional<String> userAgent;
    private final JbhRetryConfig retryConfig;
    private final Map<String,String> customHeaders;

    private JbhHttpClientConfig(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.requestTimeout = builder.requestTimeout;
        this.readTimeout = builder.readTimeout;
        this.maxConnections = builder.maxConnections;
        this.maxConnectionsPerHost = builder.maxConnectionsPerHost;
        this.followRedirects = builder.followRedirects;
        this.enableHttp2 = builder.enableHttp2;
        this.enableCompression = builder.enableCompression;
        this.userAgent = Optional.ofNullable(builder.userAgent);
        this.retryConfig = builder.retryConfig;
        this.customHeaders = builder.customHeaders;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static JbhHttpClientConfig defaultConfig() {
        return builder().build();
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public boolean isEnableCompression() {
        return enableCompression;
    }

    public Optional<String> getUserAgent() {
        return userAgent;
    }

    public JbhRetryConfig getRetryConfig() {
        return retryConfig;
    }

    public Map<String,String> getCustomHeaders() {
      return customHeaders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JbhHttpClientConfig that = (JbhHttpClientConfig) o;
        return maxConnections == that.maxConnections &&
                maxConnectionsPerHost == that.maxConnectionsPerHost &&
                followRedirects == that.followRedirects &&
                enableHttp2 == that.enableHttp2 &&
                enableCompression == that.enableCompression &&
                Objects.equals(connectTimeout, that.connectTimeout) &&
                Objects.equals(requestTimeout, that.requestTimeout) &&
                Objects.equals(readTimeout, that.readTimeout) &&
                Objects.equals(userAgent, that.userAgent) &&
                Objects.equals(retryConfig, that.retryConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectTimeout, requestTimeout, readTimeout, maxConnections,
                maxConnectionsPerHost, followRedirects, enableHttp2, enableCompression,
                userAgent, retryConfig);
    }

    @Override
    public String toString() {
        return "HttpClientConfig{" +
                "connectTimeout=" + connectTimeout +
                ", requestTimeout=" + requestTimeout +
                ", readTimeout=" + readTimeout +
                ", maxConnections=" + maxConnections +
                ", maxConnectionsPerHost=" + maxConnectionsPerHost +
                ", followRedirects=" + followRedirects +
                ", enableHttp2=" + enableHttp2 +
                ", enableCompression=" + enableCompression +
                ", userAgent=" + userAgent +
                ", retryConfig=" + retryConfig +
                '}';
    }

    public static class Builder {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private Duration readTimeout = Duration.ofSeconds(30);
        private int maxConnections = 100;
        private int maxConnectionsPerHost = 20;
        private boolean followRedirects = true;
        private boolean enableHttp2 = true;
        private boolean enableCompression = true;
        private String userAgent;
        private JbhRetryConfig retryConfig = JbhRetryConfig.defaultConfig();
        private Map<String,String> customHeaders = new HashMap<>();

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout, "Connect timeout cannot be null");
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "Request timeout cannot be null");
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = Objects.requireNonNull(readTimeout, "Read timeout cannot be null");
            return this;
        }

        public Builder maxConnections(int maxConnections) {
            if (maxConnections <= 0) {
                throw new IllegalArgumentException("Max connections must be positive");
            }
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder maxConnectionsPerHost(int maxConnectionsPerHost) {
            if (maxConnectionsPerHost <= 0) {
                throw new IllegalArgumentException("Max connections per host must be positive");
            }
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }

        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder enableHttp2(boolean enableHttp2) {
            this.enableHttp2 = enableHttp2;
            return this;
        }

        public Builder enableCompression(boolean enableCompression) {
            this.enableCompression = enableCompression;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder retryConfig(JbhRetryConfig retryConfig) {
            this.retryConfig = Objects.requireNonNull(retryConfig, "Retry config cannot be null");
            return this;
        }

        public Builder addCustomHeader(String key, String value) {
          customHeaders.put(key, value);
          return this;
        }

        public Builder addCustomHeaders(Map<String,String> customHeaders) {
          this.customHeaders.putAll(customHeaders);
          return this;
        }

        public JbhHttpClientConfig build() {
            return new JbhHttpClientConfig(this);
        }
    }
}