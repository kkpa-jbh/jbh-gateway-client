package com.jbh.gateway.client.users;

import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import java.util.Map;

public interface JbhUserGatewayClient {
  JbhHttpResponse findUserId(Map<String, String> metadata) throws JbhGatewayException;
}
