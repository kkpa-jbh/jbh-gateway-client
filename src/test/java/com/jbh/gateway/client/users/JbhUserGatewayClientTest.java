package com.jbh.gateway.client.users;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JbhUserGatewayClientTest {
  static JbhUserGatewayClient userClient;

  // Not a real JWT. The client only checks the "Bearer " prefix.
  static final String GOOD_TOKEN = "Bearer fake.jwt.token";

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

  @Test
  public void shouldNotPutTheTokenInTheErrorMessage() {
    JbhGatewayException e =
        Assertions.assertThrows(
            JbhGatewayException.class,
            () -> userClient.findUserId(Map.of("Authorization", "secret-value")));

    Assertions.assertFalse(e.getMessage().contains("secret-value"));
  }

  @Test
  public void shouldFindUserIdByAuthorizationHeader() throws IOException, InterruptedException {
    UUID userId = UUID.randomUUID();
    try (MockWebServer server = new MockWebServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "application/json")
              .setBody("\"" + userId + "\""));
      server.start();
      JbhUserGatewayClient client =
          JbhGatewayClientBuilder.builder()
              .baseUrl(server.url("/").toString())
              .sourceService("test-service")
              .build()
              .getUserClient();

      JbhHttpResponse response = client.findUserId(Map.of("Authorization", GOOD_TOKEN));

      assertEquals(200, response.getStatusCode());
      assertEquals(Optional.of(userId), response.getBodyAs(UUID.class));

      RecordedRequest request = server.takeRequest();
      assertEquals("GET", request.getMethod());
      assertEquals("/jbh-api/users/find-user-id", request.getPath());
      assertEquals(GOOD_TOKEN, request.getHeader("Authorization"));
      assertEquals("test-service", request.getHeader("JBH-XXX-REQ-SOURCE"));
    } catch (JbhGatewayException e) {
      throw new RuntimeException(e);
    }
  }
}
