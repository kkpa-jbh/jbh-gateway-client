package com.jbh.gateway.client;

import com.jbh.gateway.client.users.JbhUserGatewayClient;
import com.jbh.gateway.internal.domains.users.JbhUserApiClientImpl;
import java.time.Duration;

public class JbhGatewayClient {
  private final String baseUrl;
  private final String sourceService;
  private final Duration connectTimeout;
  private final Duration requestTimeout;

  private JbhGatewayClient(final Builder builder) {
    this.baseUrl = builder.baseUrl;
    this.sourceService = builder.sourceService;
    this.connectTimeout = builder.connectTimeout;
    this.requestTimeout = builder.requestTimeout;
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

  public JbhUserGatewayClient getUserClient() {
    return new JbhUserApiClientImpl(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String baseUrl;
    private String sourceService;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration requestTimeout = Duration.ofSeconds(30);

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

    public JbhGatewayClient build() {
      return new JbhGatewayClient(this);
    }
  }
}
