package com.jbh.gateway.client;

import com.jbh.gateway.client.notifications.JbhNotificationGatewayClient;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import com.jbh.gateway.internal.domains.notifications.JbhNotificationGatewayClientImpl;
import com.jbh.gateway.internal.domains.users.JbhUserGatewayClientImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import java.time.Duration;

public class JbhGatewayClientBuilder {
  private final String baseUrl;
  private final String sourceService;
  private final Duration connectTimeout;
  private final Duration requestTimeout;
  private final MeterRegistry meterRegistry;
  private final OpenTelemetry openTelemetry;

  private JbhGatewayClientBuilder(final Builder builder) {
    this.baseUrl = builder.baseUrl;
    this.sourceService = builder.sourceService;
    this.connectTimeout = builder.connectTimeout;
    this.requestTimeout = builder.requestTimeout;
    this.meterRegistry = builder.meterRegistry;
    this.openTelemetry = builder.openTelemetry;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getSourceService() {
    return sourceService;
  }

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  /** Where the circuit breaker metrics go, or {@code null} for no metrics. */
  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  /** The instance used for tracing. Never {@code null}: the default is {@code OpenTelemetry.noop()}. */
  public OpenTelemetry getOpenTelemetry() {
    return openTelemetry;
  }

  public JbhUserGatewayClient getUserClient() throws JbhGatewayException {
    return new JbhUserGatewayClientImpl(this);
  }

  public JbhNotificationGatewayClient getNotificationClient() throws JbhGatewayException {
    return new JbhNotificationGatewayClientImpl(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String baseUrl;
    private String sourceService;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration requestTimeout = Duration.ofSeconds(30);
    private MeterRegistry meterRegistry;
    private OpenTelemetry openTelemetry = OpenTelemetry.noop();

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder sourceService(String sourceService) {
      this.sourceService = sourceService;
      return this;
    }

    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    public Builder requestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    /**
     * Publishes the circuit breaker metrics of every client made by this builder. Optional: without
     * it the breakers still work, but they publish no metrics.
     */
    public Builder meterRegistry(MeterRegistry meterRegistry) {
      this.meterRegistry = meterRegistry;
      return this;
    }

    /**
     * Instance used to trace every call. Optional: the default (and {@code null}) is no-op, so no
     * span and no trace header. The caller owns the SDK. {@code GlobalOpenTelemetry} is never read.
     */
    public Builder openTelemetry(OpenTelemetry openTelemetry) {
      this.openTelemetry = openTelemetry == null ? OpenTelemetry.noop() : openTelemetry;
      return this;
    }

    public JbhGatewayClientBuilder build() {
      return new JbhGatewayClientBuilder(this);
    }
  }
}
