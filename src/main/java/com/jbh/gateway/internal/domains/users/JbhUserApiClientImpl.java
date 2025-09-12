package com.jbh.gateway.internal.domains.users;

import com.jbh.gateway.client.JbhGatewayClient;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import com.jbh.gateway.internal.domains.BaseApiGatewayClient;
import java.util.Map;

public class JbhUserApiClientImpl extends BaseApiGatewayClient implements JbhUserGatewayClient {

  private final static String FIND_USER_ID_PATH = "/users/find-user-id";

  public JbhUserApiClientImpl(final JbhGatewayClient hostConfig) {
   super(hostConfig);
  }

  @Override
  public JbhHttpResponse findUserId(final Map<String, String> metadata) throws JbhGatewayException {
    JbhHttpHeaders jbhHttpHeaders = JbhHttpHeaders.fromMap(metadata);

    validator.validateAuthentication(jbhHttpHeaders);

    return getCall(FIND_USER_ID_PATH, jbhHttpHeaders);
  }
}
