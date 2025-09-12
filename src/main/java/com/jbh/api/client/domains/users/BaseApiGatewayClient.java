package com.jbh.api.client.domains.users;

import com.jbh.api.client.JbhGatewayClient;
import com.jbh.api.client.JbhGatewayClientImpl;
import com.jbh.api.client.api.JbhGatewayHostConfig;
import com.jbh.api.client.core.JbhHttpValidator;
import com.jbh.api.client.core.JbhHttpValidatorImpl;
import com.jbh.api.client.core.http.AdapterType;

public class BaseApiGatewayClient {
  protected  JbhHttpValidator validator;

  protected JbhGatewayClient jbhGatewayClient;
  private final JbhGatewayHostConfig hostConfig;

  public BaseApiGatewayClient(JbhGatewayHostConfig hostConfig) {
    this.hostConfig = hostConfig;
    this.validator = new JbhHttpValidatorImpl();
    this.jbhGatewayClient = new JbhGatewayClientImpl(getGatewayHostUrl(), AdapterType.NATIVE);
  }

  protected String getGatewayHostUrl() {
    return hostConfig.getBaseUrl();
  }
}
