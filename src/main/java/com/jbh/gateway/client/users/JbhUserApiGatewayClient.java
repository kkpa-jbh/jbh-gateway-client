package com.jbh.gateway.client.users;

import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.internal.core.http.model.JbhHttpResponse;
import java.util.Map;

public interface JbhUserApiGatewayClient {
  JbhHttpResponse findUserId(Map<String, String> metadata) throws JbhGatewayException;
}
