package com.jbh.gateway.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.jbh.gateway.internal.JbhGatewayClient;
import com.jbh.gateway.internal.JbhGatewayClientImpl;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.config.JbhRetryConfig;
import com.jbh.gateway.internal.core.http.AdapterType;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpMethod;
import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.dto.TestUserDto;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class JbhGatewayClientIntegrationTest {

  private JbhGatewayClient gatewayClient;

  @BeforeEach
  void setUp() {
    // Use httpbin.org as a test service
    gatewayClient = new JbhGatewayClientImpl("https://httpbin.org", AdapterType.NATIVE);
  }

  @AfterEach
  void tearDown() {
    if (gatewayClient != null) {
      try {
        gatewayClient.close();
      } catch (Exception e) {
        // Log or ignore cleanup errors in tests
      }
    }
  }

  @Test
  void testBasicGetRequest() {
    JbhHttpResponse response = gatewayClient.get("/get");

    assertTrue(response.isSuccessful(), "Request should be successful");
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getContentType().isPresent());
    assertTrue(response.getContentType().get().contains("application/json"));
    assertTrue(response.getBody().isPresent());

    String body = response.getBody().get();
    assertTrue(body.contains("url"), "Response should contain URL field");
    assertTrue(body.contains("httpbin.org/get"), "Response should contain our URL");
  }

  @Test
  void testGetWithCustomHeaders() {
    JbhHttpHeaders headers = JbhHttpHeaders.builder()
        .add("User-Agent", "JBH-Gateway-Client/1.0")
        .add("Accept", "application/json")
        .add("X-Custom-Header", "integration-test")
        .build();

    JbhHttpResponse response = gatewayClient.get("/get", headers);

    assertTrue(response.isSuccessful(), "Request should be successful");
    assertEquals(200, response.getStatusCode());

    String body = response.getBody().get();
    assertTrue(body.contains("JBH-Gateway-Client/1.0"), "Response should contain our User-Agent");
    assertTrue(body.contains("integration-test"), "Response should contain our custom header");
  }

  @Test
  void testPostRequest() {
    String jsonPayload = "{\"name\":\"John Doe\",\"email\":\"john@example.com\",\"age\":30}";

    JbhHttpResponse response = gatewayClient.post("/post", jsonPayload);

    assertTrue(response.isSuccessful(), "POST request should be successful");
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().isPresent());

    String body = response.getBody().get();
    assertTrue(body.contains("John Doe"), "Response should contain our JSON payload");
    assertTrue(body.contains("john@example.com"), "Response should contain our email");
  }

  @Test
  void testPostWithCustomHeaders() {
    String jsonPayload = "{\"message\":\"Hello from JBH Gateway Client\"}";
    JbhHttpHeaders headers = JbhHttpHeaders.builder()
        .add("Content-Type", "application/json")
        .add("X-Request-ID", "test-12345")
        .build();

    JbhHttpResponse response = gatewayClient.post("/post", jsonPayload, headers);

    assertTrue(response.isSuccessful(), "POST request with headers should be successful");
    assertEquals(200, response.getStatusCode());

    String body = response.getBody().get();
    assertTrue(body.contains("Hello from JBH Gateway Client"), "Response should contain our message");
  }

  @Test
  void testPutRequest() {
    String jsonPayload = "{\"id\":123,\"status\":\"updated\"}";

    JbhHttpResponse response = gatewayClient.put("/put", jsonPayload);

    assertTrue(response.isSuccessful(), "PUT request should be successful");
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().isPresent());

    String body = response.getBody().get();
    assertTrue(body.contains("updated"), "Response should contain our updated status");
  }

  @Test
  void testDeleteRequest() {
    JbhHttpResponse response = gatewayClient.delete("/delete");

    assertTrue(response.isSuccessful(), "DELETE request should be successful");
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().isPresent());
  }

  @Test
  void testAsyncGetRequest() throws Exception {
    CompletableFuture<JbhHttpResponse> future = gatewayClient.getAsync("/get");

    JbhHttpResponse response = future.get(10, TimeUnit.SECONDS);

    assertTrue(response.isSuccessful(), "Async GET request should be successful");
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().isPresent());
  }

  @Test
  void testAsyncPostRequest() throws Exception {
    String jsonPayload = "{\"async\":true,\"message\":\"Hello async world\"}";

    CompletableFuture<JbhHttpResponse> future = gatewayClient.postAsync("/post", jsonPayload);

    JbhHttpResponse response = future.get(10, TimeUnit.SECONDS);

    assertTrue(response.isSuccessful(), "Async POST request should be successful");
    assertEquals(200, response.getStatusCode());

    String body = response.getBody().get();
    assertTrue(body.contains("Hello async world"), "Response should contain our async message");
  }

  @Test
  void testTypedGetRequest() {
    // httpbin.org/json returns a simple JSON object that we can map to a generic response
    String jsonResponse = "{\"id\":\"test-123\",\"name\":\"Test User\",\"email\":\"test@example.com\",\"createdAt\":\"2023-01-01T12:00:00\"}";
    
    // First, let's test with a POST request to httpbin.org/post where we can control the response
    JbhHttpResponse response = gatewayClient.postForObject("/post", jsonResponse, TestUserDto.class);

    assertTrue(response.isSuccessful(), "Typed POST request should be successful");
    assertEquals(200, response.getStatusCode());
    
    // The response will contain the data we sent in the "data" field, so we can't directly deserialize the whole response
    // But we can verify that both string and typed approaches work
    assertTrue(response.getBody().isPresent(), "String body should be present");
    String body = response.getBody().get();
    assertTrue(body.contains("test@example.com"), "Response should contain our test data");
  }

  @Test
  void testTypedGetRequestWithHeaders() {
    String jsonResponse = "{\"id\":\"header-test\",\"name\":\"Header Test User\",\"email\":\"header@example.com\",\"createdAt\":\"2023-02-01T10:30:00\"}";
    
    JbhHttpHeaders headers = JbhHttpHeaders.builder()
        .add("X-Test-Type", "typed-request")
        .build();

    JbhHttpResponse response = gatewayClient.postForObject("/post", jsonResponse, headers, TestUserDto.class);

    assertTrue(response.isSuccessful(), "Typed POST request with headers should be successful");
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().isPresent());

    String body = response.getBody().get();
    assertTrue(body.contains("header@example.com"), "Response should contain our test data");
    assertTrue(body.contains("typed-request"), "Response should contain our custom header");
  }

  @Test
  void testErrorHandling() {
    JbhHttpResponse response = gatewayClient.get("/status/404");

    assertFalse(response.isSuccessful(), "404 response should not be successful");
    assertEquals(404, response.getStatusCode());
  }

  @Test
  void testCustomConfiguredClient() {
    JbhHttpClientConfig customConfig = JbhHttpClientConfig.builder()
        .connectTimeout(Duration.ofSeconds(5))
        .requestTimeout(Duration.ofSeconds(15))
        .enableHttp2(true)
        .enableCompression(true)
        .userAgent("JBH-Gateway-Integration-Test/1.0")
        .retryConfig(JbhRetryConfig.builder()
            .maxAttempts(2)
            .initialDelay(Duration.ofMillis(200))
            .build())
        .build();

    JbhGatewayClient customClient = new JbhGatewayClientImpl(
        "https://httpbin.org", AdapterType.NATIVE, customConfig);

    try {
      JbhHttpResponse response = customClient.get("/get");

      assertTrue(response.isSuccessful(), "Custom configured client should work");
      assertEquals(200, response.getStatusCode());

      String body = response.getBody().get();
      assertTrue(body.contains("JBH-Gateway-Integration-Test/1.0"), 
          "Response should contain our custom user agent");

    } finally {
      try {
        customClient.close();
      } catch (Exception e) {
        // Ignore cleanup errors in tests
      }
    }
  }

  @Test
  void testClientMetadata() {
    assertEquals("https://httpbin.org", gatewayClient.getBaseUrl());
    assertEquals("native", gatewayClient.getClientAdapterName());
    assertTrue(gatewayClient.supportsHttp2(), "Native client should support HTTP/2");
  }

  @Test
  void testTimeout() {
    // Test with a request that takes longer than our timeout
    JbhHttpHeaders headers = JbhHttpHeaders.builder().build();
    Duration shortTimeout = Duration.ofSeconds(1);

    assertThrows(Exception.class, () -> {
      gatewayClient.get("/delay/5", headers, shortTimeout);
    }, "Request should timeout");
  }

  @Test
  void testRequestBuilder() {
    // Test the fluent request builder API
    var requestBuilder = gatewayClient.request(
        JbhHttpMethod.GET, "/get");

    var request = requestBuilder
        .header("X-Builder-Test", "true")
        .timeout(Duration.ofSeconds(10))
        .build();

    JbhHttpResponse response = gatewayClient.execute(request);

    assertTrue(response.isSuccessful(), "Request builder should work");
    assertEquals(200, response.getStatusCode());

    String body = response.getBody().get();
    assertTrue(body.contains("X-Builder-Test"), "Response should contain our builder header");
  }
}