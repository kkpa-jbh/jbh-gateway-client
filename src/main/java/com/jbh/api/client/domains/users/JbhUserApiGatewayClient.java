package com.jbh.api.client.domains.users;

import com.jbh.api.client.api.JbhApiException;
import com.jbh.api.client.core.http.model.JbhHttpResponse;
import java.util.Map;

public interface JbhUserApiGatewayClient {
  JbhHttpResponse findUserId(Map<String, String> metadata) throws JbhApiException;
}
