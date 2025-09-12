package com.jbh.api.client.core;

import com.jbh.api.client.http.model.JbhHttpHeaders;
import com.jbh.api.client.vo.JbhApiException;

public class JbhHttpValidatorImpl implements JbhHttpValidator{

  @Override
  public void validateAuthentication(JbhHttpHeaders headers) throws JbhApiException {

    headers.validateAuthHeader();
  }
}
