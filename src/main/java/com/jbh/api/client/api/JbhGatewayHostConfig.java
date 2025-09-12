package com.jbh.api.client.api;

public class JbhGatewayHostConfig {
  private final String baseUrl;

  private JbhGatewayHostConfig(final Builder builder) {
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

    public JbhGatewayHostConfig build() {
      return new JbhGatewayHostConfig(this);
    }
  }
}
