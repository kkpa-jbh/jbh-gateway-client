package com.jbh.gateway.internal.core.validators;

import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.client.JbhGatewayException;

public interface JbhHttpValidator {

  void validateAuthorizationHeader(JbhHttpHeaders headers) throws JbhGatewayException;

  void vaildateClientSourceHeader(JbhHttpHeaders headers) throws JbhGatewayException;

}
