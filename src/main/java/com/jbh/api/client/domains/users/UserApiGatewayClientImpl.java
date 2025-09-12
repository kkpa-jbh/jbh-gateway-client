package com.jbh.api.client.domains.users;

import com.jbh.api.client.api.JbhGatewayHostConfig;
import com.jbh.api.client.core.JbhHttpValidator;
import com.jbh.api.client.core.http.model.JbhHttpHeaders;
import com.jbh.api.client.api.JbhApiException;
import java.util.Map;

public class UserApiGatewayClientImpl extends BaseApiGatewayClient implements UserApiGatewayClient{

  private final static String FIND_USER_ID_PATH = "/users/find-user-id";

  public UserApiGatewayClientImpl(final JbhGatewayHostConfig hostConfig) {
   super(hostConfig);
  }

  @Override
  public String findUserId(final Map<String, String> metadata) throws JbhApiException {
    JbhHttpHeaders jbhHttpHeaders = JbhHttpHeaders.fromMap(metadata);

    validator.validateAuthentication(jbhHttpHeaders);

    return "";
  }
}
