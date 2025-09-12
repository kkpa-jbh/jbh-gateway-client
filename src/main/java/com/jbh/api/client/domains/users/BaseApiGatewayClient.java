package com.jbh.api.client.domains.users;

import com.jbh.api.client.api.JbhGatewayHostConfig;
import com.jbh.api.client.core.JbhHttpValidator;
import com.jbh.api.client.core.JbhHttpValidatorImpl;

public class BaseApiGatewayClient {
  protected  JbhHttpValidator validator;

  private final JbhGatewayHostConfig hostConfig;

  public BaseApiGatewayClient(JbhGatewayHostConfig hostConfig) {
    this.hostConfig = hostConfig;
    this.validator = new JbhHttpValidatorImpl();
  }
}
