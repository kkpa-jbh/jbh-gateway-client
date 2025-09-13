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
      "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyYWZiZGNmYy1lYmM2LTQxODAtOGE2My02MDljZjhmMTk4YmEiLCJpYXQiOjE3NTc1ODE2NjgsImV4cCI6MTc1ODE4NjQ2OH0.j0RbO6jojm_Rw-RHdlbOCsJM6uToZu2Uphw3Q67V_r5_s6nuv_CLPuFeIXYUASpwweNdgSFMOEpPLSqQLKfrJw";

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
