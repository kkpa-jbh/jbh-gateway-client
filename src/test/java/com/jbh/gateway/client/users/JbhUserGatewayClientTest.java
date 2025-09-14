package com.jbh.gateway.client.users;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JbhUserGatewayClientTest {
  static JbhUserGatewayClient userClient;

  static final String GOOD_TOKEN =
      "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyYWZiZGNmYy1lYmM2LTQxODAtOGE2My02MDljZjhmMTk4YmEiLCJpYXQiOjE3NTc4NDQyNjksImV4cCI6MTc1ODQ0OTA2OX0.r1qQJATRVVk9mw5twn66c-M-aszjgZ96H9cmsAV9W9-STEj0Da80vb_J3Zm8M8ZAac_1Lt8wMTdAedpaDV5hIg";

  @BeforeAll
  public static void setup() {
    JbhGatewayClientBuilder clientBuilder =
        JbhGatewayClientBuilder.builder()
            .baseUrl("http://localhost:8080")
            .sourceService("test-service")
            .build();

    try {
      userClient = clientBuilder.getUserClient();
    } catch (JbhGatewayException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void shouldThrowExceptionWHenNotValidAuthorizationHeader() {

    String TOKEN_WITHOUT_BEARER = "eyJhbGciOiJIUzUxMiJ9..";

    Assertions.assertThrows(
        JbhGatewayException.class,
        () -> {
          userClient.findUserId(Map.of("Authorization", TOKEN_WITHOUT_BEARER));
        });
  }

  public void shouldFindUserIdByAuthorizationHeader() {
    try {
      JbhHttpResponse response = userClient.findUserId(Map.of("Authorization", GOOD_TOKEN));
      assertEquals(200, response.getStatusCode());
    } catch (JbhGatewayException e) {
      throw new RuntimeException(e);
    }
  }
}
