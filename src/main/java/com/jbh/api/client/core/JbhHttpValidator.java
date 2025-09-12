package com.jbh.api.client.core;

import com.jbh.api.client.http.model.JbhHttpHeaders;
import com.jbh.api.client.http.model.JbhHttpRequest;
import com.jbh.api.client.vo.JbhApiException;

public interface JbhHttpValidator {

  void validateAuthentication(JbhHttpHeaders headers) throws JbhApiException;

}
