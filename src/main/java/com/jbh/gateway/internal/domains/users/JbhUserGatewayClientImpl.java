package com.jbh.gateway.internal.domains.users;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.domains.BaseApiGatewayClient;
import java.util.Map;

public class JbhUserGatewayClientImpl extends BaseApiGatewayClient implements JbhUserGatewayClient {

  private static final String FIND_USER_ID_PATH = "/users/find-user-id";

  public JbhUserGatewayClientImpl(final JbhGatewayClientBuilder hostConfig)
      throws JbhGatewayException {
    super(hostConfig);
  }

  @Override
  public JbhHttpResponse findUserId(final Map<String, String> clientHttpHeaders)
      throws JbhGatewayException {
    JbhHttpHeaders clientJbhHttpHeaders = JbhHttpHeaders.fromMap(clientHttpHeaders);
    validator.validateAuthorizationHeader(clientJbhHttpHeaders);

    return executeGet(FIND_USER_ID_PATH, clientJbhHttpHeaders);
  }
}
