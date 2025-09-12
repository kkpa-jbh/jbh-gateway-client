package com.jbh.api.client.domains.users;

import com.jbh.api.client.api.JbhApiException;
import java.util.Map;

public interface UserApiGatewayClient {
  String findUserId(Map<String, String> metadata) throws JbhApiException;
}
