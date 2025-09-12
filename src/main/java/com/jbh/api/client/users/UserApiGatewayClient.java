package com.jbh.api.client.users;

import com.jbh.api.client.vo.JbhApiException;
import java.util.Map;

public interface UserApiGatewayClient {
  String findUserId(Map<String, String> metadata) throws JbhApiException;
}
