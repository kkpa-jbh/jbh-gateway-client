package com.jbh.gateway.internal.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.JbhGatewayInternalClientImpl;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.config.JbhRetryConfig;
import com.jbh.gateway.internal.core.http.adapter.NativeHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Calls httpbin.org over the network. Excluded from {@code mvn test}. Run it with {@code mvn test
 * -Dgroups=integration -DexcludedGroups=}.
 */
@Tag("integration")
class JbhGatewayClientIntegrationTest {

  private static final String BASE_URL = "https://httpbin.org";

  private JbhGatewayInternalClientImpl gatewayClient;

  @BeforeEach
  void setUp() {
    gatewayClient = newClient(JbhHttpClientConfig.defaultConfig());
  }

  private static JbhGatewayInternalClientImpl newClient(JbhHttpClientConfig config) {
    return new JbhGatewayInternalClientImpl(BASE_URL, config, new NativeHttpClientAdapter(config));
  }

  @Test
  void testBasicGetRequest() throws Exception {
    JbhHttpResponse response = gatewayClient.get("/get", JbhHttpHeaders.empty());

    assertTrue(response.isSuccessful(), "Request should be successful");
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getContentType().isPresent());
    assertTrue(response.getContentType().get().contains("application/json"));

    String body = response.getBody().get();
    assertTrue(body.contains("httpbin.org/get"), "Response should contain our URL");
  }

  @Test
  void testGetWithCustomHeaders() throws Exception {
    JbhHttpHeaders headers =
        JbhHttpHeaders.builder()
            .add("Accept", "application/json")
            .add("X-Custom-Header", "integration-test")
            .build();

    JbhHttpResponse response = gatewayClient.get("/get", headers);

    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().get().contains("integration-test"));
  }

  @Test
  void testPostRequest() throws Exception {
    String jsonPayload = "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}";

    JbhHttpResponse response = gatewayClient.post("/post", jsonPayload, JbhHttpHeaders.empty());

    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().get().contains("john@example.com"));
  }

  @Test
  void testPutRequest() throws Exception {
    String jsonPayload = "{\"id\":123,\"status\":\"updated\"}";

    JbhHttpResponse response = gatewayClient.put("/put", jsonPayload, JbhHttpHeaders.empty());

    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().get().contains("updated"));
  }

  @Test
  void testDeleteRequest() throws Exception {
    JbhHttpResponse response = gatewayClient.delete("/delete", JbhHttpHeaders.empty());

    assertEquals(200, response.getStatusCode());
  }

  @Test
  void testErrorStatusIsAResponse() throws Exception {
    JbhHttpResponse response = gatewayClient.get("/status/404", JbhHttpHeaders.empty());

    assertFalse(response.isSuccessful(), "404 response should not be successful");
    assertEquals(404, response.getStatusCode());
  }

  @Test
  void testTimeout() {
    JbhHttpClientConfig config =
        JbhHttpClientConfig.builder()
            .requestTimeout(Duration.ofSeconds(1))
            .retryConfig(JbhRetryConfig.noRetries())
            .build();
    JbhGatewayInternalClientImpl shortTimeoutClient = newClient(config);

    assertThrows(
        java.net.http.HttpTimeoutException.class,
        () -> shortTimeoutClient.get("/delay/5", JbhHttpHeaders.empty()),
        "Request should timeout");
  }
}
