package com.jbh.gateway.internal.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for the HTTP client adapter: timeouts, redirects, HTTP/2, retries and default
 * headers. Supports a fluent builder.
 */
public final class JbhHttpClientConfig {

    private Duration connectTimeout;
    private Duration requestTimeout;
    private final boolean followRedirects;
    private final boolean enableHttp2;
    private final JbhRetryConfig retryConfig;
    private  Map<String,String> customHeaders;

    private JbhHttpClientConfig(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.requestTimeout = builder.requestTimeout;
        this.followRedirects = builder.followRedirects;
        this.enableHttp2 = builder.enableHttp2;
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

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public JbhRetryConfig getRetryConfig() {
        return retryConfig;
    }

    public Map<String,String> getCustomHeaders() {
      return customHeaders;
    }

    @Override
    public String toString() {
        return "HttpClientConfig{" +
                "connectTimeout=" + connectTimeout +
                ", requestTimeout=" + requestTimeout +
                ", followRedirects=" + followRedirects +
                ", enableHttp2=" + enableHttp2 +
                ", retryConfig=" + retryConfig +
                '}';
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }

    public static class Builder {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private boolean followRedirects = true;
        private boolean enableHttp2 = true;
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

        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder enableHttp2(boolean enableHttp2) {
            this.enableHttp2 = enableHttp2;
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
