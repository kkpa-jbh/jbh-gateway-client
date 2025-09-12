package com.jbh.api.client.domains.users;

import com.jbh.api.client.JbhGatewayClient;
import com.jbh.api.client.JbhGatewayClientImpl;
import com.jbh.api.client.api.JbhGatewayHostConfig;
import com.jbh.api.client.core.JbhHttpValidator;
import com.jbh.api.client.core.JbhHttpValidatorImpl;
import com.jbh.api.client.core.http.AdapterType;

public class BaseApiGatewayClient {
  protected final JbhHttpValidator validator;
  protected final JbhGatewayClient jbhGatewayClient;
  private final JbhGatewayHostConfig hostConfig;

  public BaseApiGatewayClient(JbhGatewayHostConfig hostConfig) {
    this(hostConfig, new JbhHttpValidatorImpl());
  }

  public BaseApiGatewayClient(JbhGatewayHostConfig hostConfig, JbhHttpValidator validator) {
    this.hostConfig = hostConfig;
    this.validator = validator;
    this.jbhGatewayClient = new JbhGatewayClientImpl(getGatewayHostUrl(), AdapterType.NATIVE);
  }

  protected String getGatewayHostUrl() {
    return hostConfig.getBaseUrl();
  }
}
