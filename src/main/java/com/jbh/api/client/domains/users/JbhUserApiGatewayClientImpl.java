package com.jbh.api.client.domains.users;

import com.jbh.api.client.api.JbhGatewayHostConfig;
import com.jbh.api.client.core.http.model.JbhHttpHeaders;
import com.jbh.api.client.api.JbhApiException;
import com.jbh.api.client.core.http.model.JbhHttpResponse;
import java.util.Map;

public class JbhUserApiGatewayClientImpl extends BaseApiGatewayClient implements JbhUserApiGatewayClient {

  private final static String FIND_USER_ID_PATH = "/users/find-user-id";

  public JbhUserApiGatewayClientImpl(final JbhGatewayHostConfig hostConfig) {
   super(hostConfig);
  }

  @Override
  public JbhHttpResponse findUserId(final Map<String, String> metadata) throws JbhApiException {
    JbhHttpHeaders jbhHttpHeaders = JbhHttpHeaders.fromMap(metadata);

    validator.validateAuthentication(jbhHttpHeaders);

    return jbhGatewayClient.get(FIND_USER_ID_PATH, jbhHttpHeaders);
  }
}
