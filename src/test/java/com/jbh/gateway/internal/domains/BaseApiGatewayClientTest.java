package com.jbh.gateway.internal.domains;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.users.JbhUserGatewayClient;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.domains.users.JbhUserGatewayClientImpl;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Network errors become JbhGatewayException with one error code. */
class BaseApiGatewayClientTest {

  private static final Map<String, String> AUTH = Map.of("Authorization", "Bearer fake-token");

  private static JbhGatewayClientBuilder config(String baseUrl) {
    return JbhGatewayClientBuilder.builder().baseUrl(baseUrl).sourceService("test-service").build();
  }

  private static JbhGatewayException callWith(JbhHttpClientAdapter adapter) throws Exception {
    JbhUserGatewayClient client = new JbhUserGatewayClientImpl(config("http://localhost:8080"), adapter);
    return assertThrows(JbhGatewayException.class, () -> client.findUserId(AUTH));
  }

  @Test
  void connectExceptionBecomesConnectionFailed() throws Exception {
    JbhGatewayException e =
        callWith(
            request -> {
              throw new ConnectException("refused");
            });

    assertEquals(BaseApiGatewayClient.CONNECTION_FAILED, e.getErrorCode());
    assertNull(e.getHttpStatusCode());
    assertInstanceOf(ConnectException.class, e.getCause());
    assertTrue(e.getMessage().contains("GET /jbh-api/users/find-user-id"));
  }

  @Test
  void timeoutBecomesTimeout() throws Exception {
    JbhGatewayException e =
        callWith(
            request -> {
              throw new HttpTimeoutException("timed out");
            });

    assertEquals(BaseApiGatewayClient.TIMEOUT, e.getErrorCode());
  }

  @Test
  void otherIoErrorBecomesIoError() throws Exception {
    JbhGatewayException e =
        callWith(
            request -> {
              throw new IOException("connection reset");
            });

    assertEquals(BaseApiGatewayClient.IO_ERROR, e.getErrorCode());
  }

  @Test
  void interruptBecomesInterruptedAndKeepsTheFlag() throws Exception {
    try {
      JbhGatewayException e =
          callWith(
              request -> {
                throw new InterruptedException();
              });

      assertEquals(BaseApiGatewayClient.INTERRUPTED, e.getErrorCode());
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void refusedConnectionThroughPublicApiBecomesConnectionFailed() throws Exception {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    JbhUserGatewayClient client = config("http://127.0.0.1:" + closedPort).getUserClient();

    JbhGatewayException e = assertThrows(JbhGatewayException.class, () -> client.findUserId(AUTH));

    assertEquals(BaseApiGatewayClient.CONNECTION_FAILED, e.getErrorCode());
  }
}
