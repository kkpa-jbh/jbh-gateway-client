package com.jbh.gateway.internal.core.validators;

import static com.jbh.gateway.internal.core.http.model.JbhHttpHeaders.AUTHORIZATION_HEADER;
import static com.jbh.gateway.internal.core.http.model.JbhHttpHeaders.BEARER_TOKEN_PREFIX;
import static com.jbh.gateway.internal.core.http.model.JbhHttpHeaders.REQ_SOURCE_HEADER;

import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;

public class JbhHttpValidatorImpl implements JbhHttpValidator {

  @Override
  public void validateAuthorizationHeader(final JbhHttpHeaders headers) throws JbhGatewayException {
    if (!hasValidAuthorization(headers)) {
      throw new JbhGatewayException("Missing or invalid Authorization header " + headers.getFirst(AUTHORIZATION_HEADER));
    }
  }

  @Override
  public void vaildateClientSourceHeader(JbhHttpHeaders headers) throws JbhGatewayException {
    if (!headers.contains(REQ_SOURCE_HEADER)) {
      throw new JbhGatewayException("Missing microservice source header");
    }
  }

  private boolean hasValidAuthorization(final JbhHttpHeaders headers) {
    return headers.contains(AUTHORIZATION_HEADER)
        && headers
            .getFirst(AUTHORIZATION_HEADER)
            .map(value -> value.startsWith(BEARER_TOKEN_PREFIX))
            .orElse(false);
  }
}
