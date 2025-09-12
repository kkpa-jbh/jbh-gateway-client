package com.jbh.gateway.internal.domains.users;

import com.jbh.gateway.client.JbhGatewayClient;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.internal.core.http.model.JbhHttpResponse;
import com.jbh.gateway.client.users.JbhUserApiGatewayClient;
import com.jbh.gateway.internal.domains.BaseApiGatewayClient;
import java.util.Map;

public class JbhUserApiGatewayClientImpl extends BaseApiGatewayClient implements JbhUserApiGatewayClient {

  private final static String FIND_USER_ID_PATH = "/users/find-user-id";

  public JbhUserApiGatewayClientImpl(final JbhGatewayClient hostConfig) {
   super(hostConfig);
  }

  @Override
  public JbhHttpResponse findUserId(final Map<String, String> metadata) throws JbhGatewayException {
    JbhHttpHeaders jbhHttpHeaders = JbhHttpHeaders.fromMap(metadata);

    validator.validateAuthentication(jbhHttpHeaders);

    return getCall(FIND_USER_ID_PATH, jbhHttpHeaders);
  }
}
