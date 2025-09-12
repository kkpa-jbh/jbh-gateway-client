package com.jbh.gateway.internal.core.validators;

import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.client.JbhGatewayException;

public interface JbhHttpValidator {

  void validateAuthentication(JbhHttpHeaders headers) throws JbhGatewayException;

}
