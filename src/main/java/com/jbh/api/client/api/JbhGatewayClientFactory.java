package com.jbh.api.client.api;

import com.jbh.api.client.domains.users.JbhUserApiGatewayClient;
import com.jbh.api.client.domains.users.JbhUserApiGatewayClientImpl;

public class JbhGatewayClientFactory {

  public static JbhUserApiGatewayClient createUserApiClient(JbhGatewayHostConfig hostConfig) {
    return new JbhUserApiGatewayClientImpl(hostConfig);
  }

}
