package com.jbh.api.client.core;

import com.jbh.api.client.core.http.model.JbhHttpHeaders;
import com.jbh.api.client.api.JbhApiException;
import com.jbh.api.client.api.JbhHttpHeaderNames;

public class JbhHttpValidatorImpl implements JbhHttpValidator {

  @Override
  public void validateAuthentication(final JbhHttpHeaders headers) throws JbhApiException {
    if (!hasValidAuthorization(headers)) {
      throw new JbhApiException("Missing or invalid Authorization header");
    }
    
    if (!headers.contains(JbhHttpHeaderNames.REQ_SOURCE_HEADER)) {
      throw new JbhApiException("Missing microservice source header");
    }
  }
  
  private boolean hasValidAuthorization(final JbhHttpHeaders headers) {
    return headers.contains("Authorization") && 
           headers.getFirst("Authorization")
                 .map(value -> value.startsWith("Bearer "))
                 .orElse(false);
  }
}
