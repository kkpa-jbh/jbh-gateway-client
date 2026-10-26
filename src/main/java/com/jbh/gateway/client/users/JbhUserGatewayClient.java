package com.jbh.gateway.client.users;

import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import java.util.Map;
import java.util.UUID;

public interface JbhUserGatewayClient {
  JbhHttpResponse findUserId(Map<String, String> metadata) throws JbhGatewayException;

  /**
   * Asks IAM for one user group, as the user of the token. IAM answers 200 to an accepted member,
   * 403 to any other user and 404 when the group does not exist.
   */
  JbhHttpResponse findUserGroup(UUID groupId, Map<String, String> metadata)
      throws JbhGatewayException;
}
