package com.jbh.api.client.domains.users;

import com.jbh.api.client.core.JbhHttpValidator;
import com.jbh.api.client.core.http.model.JbhHttpHeaders;
import com.jbh.api.client.vo.JbhApiException;
import java.util.Map;

public class UserApiGatewayClientImpl implements UserApiGatewayClient{

  private final JbhHttpValidator validator;

  private final static String FIND_USER_ID_PATH = "/users/find-user-id";

  public UserApiGatewayClientImpl(final JbhHttpValidator validator) {
    this.validator = validator;
  }

  @Override
  public String findUserId(final Map<String, String> metadata) throws JbhApiException {
    JbhHttpHeaders jbhHttpHeaders = JbhHttpHeaders.fromMap(metadata);

    validator.validateAuthentication(jbhHttpHeaders);

    return "";
  }
}
