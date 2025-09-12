package com.jbh.api.client.config;

public class GatewayHostConfig {
  private final String baseUrl;

  private GatewayHostConfig(final Builder builder) {
    this.baseUrl = builder.baseUrl;
  }


  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String baseUrl;

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public GatewayHostConfig build() {
      return new GatewayHostConfig(this);
    }
  }
}
