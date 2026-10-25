package com.jbh.gateway.internal.domains.users;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.domains.BaseApiGatewayClient;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class JbhUserGatewayClientImpl extends BaseApiGatewayClient implements JbhUserGatewayClient {

  private static final String FIND_USER_ID_PATH = "/users/find-user-id";
  private static final String USER_GROUPS_PATH = "/user-groups/";

  public JbhUserGatewayClientImpl(final JbhGatewayClientBuilder hostConfig)
      throws JbhGatewayException {
    super(hostConfig);
  }

  /** Uses the given transport. For tests. */
  public JbhUserGatewayClientImpl(
      final JbhGatewayClientBuilder hostConfig, final JbhHttpClientAdapter httpClientAdapter)
      throws JbhGatewayException {
    super(hostConfig, httpClientAdapter);
  }

  @Override
  public JbhHttpResponse findUserId(final Map<String, String> clientHttpHeaders)
      throws JbhGatewayException {
    JbhHttpHeaders clientJbhHttpHeaders = JbhHttpHeaders.fromMap(clientHttpHeaders);
    validator.validateAuthorizationHeader(clientJbhHttpHeaders);

    return executeGet(FIND_USER_ID_PATH, clientJbhHttpHeaders, UUID.class);
  }

  @Override
  public JbhHttpResponse findUserGroup(
      final UUID groupId, final Map<String, String> clientHttpHeaders) throws JbhGatewayException {
    Objects.requireNonNull(groupId, "groupId must not be null");
    JbhHttpHeaders clientJbhHttpHeaders = JbhHttpHeaders.fromMap(clientHttpHeaders);
    validator.validateAuthorizationHeader(clientJbhHttpHeaders);

    return executeGet(USER_GROUPS_PATH + groupId, clientJbhHttpHeaders);
  }
}
