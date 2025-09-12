package com.jbh.api.client;

import com.jbh.api.client.config.JbhHttpClientConfig;
import com.jbh.api.client.config.JbhRetryConfig;
import com.jbh.api.client.core.http.AdapterType;
import com.jbh.api.client.core.http.JbhHttpClientAdapter;
import com.jbh.api.client.core.http.JbhHttpClientFactory;
import com.jbh.api.client.http.MockHttpClientAdapter;
import com.jbh.api.client.core.http.model.JbhHttpHeaders;
import com.jbh.api.client.core.http.model.JbhHttpRequest;
import com.jbh.api.client.core.http.model.JbhHttpResponse;
import com.jbh.api.client.dto.TestUserDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class JbhApiGatewayClientRunnerTest {

    private JbhHttpClientFactory factory;
    private MockHttpClientAdapter mockAdapter;

    @BeforeEach
    void setUp() {
        factory = JbhHttpClientFactory.createDefault();
        mockAdapter = new MockHttpClientAdapter();
        factory.registerAdapter(AdapterType.MOCK, config -> mockAdapter);
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.shutdown();
        }
    }

    @Test
    void testBasicUsage() {
        JbhHttpResponse mockResponse = JbhHttpResponse.builder()
                .statusCode(200)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body("{\"message\":\"success\"}")
                .build();
        
        mockAdapter.thenReturn(mockResponse);
        
        JbhHttpClientAdapter adapter = factory.createAdapter(AdapterType.MOCK);
        
        try {
            JbhHttpRequest request = JbhHttpRequest.get("https://httpbin.org/get")
                    .header("User-Agent", "jbh-api-client/1.0")
                    .header("Accept", "application/json")
                    .build();
            
            JbhHttpResponse response = adapter.execute(request);
            
            assertTrue(response.isSuccessful());
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getContentType().isPresent());
            assertEquals("application/json", response.getContentType().get());
            assertTrue(response.getBody().isPresent());
            assertEquals("{\"message\":\"success\"}", response.getBody().get());
            
        } finally {
            adapter.close();
        }
    }

    @Test
    void testCustomConfiguration() {
        JbhHttpClientConfig customConfig = JbhHttpClientConfig.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(10))
                .enableHttp2(true)
                .enableCompression(true)
                .userAgent("JBH-Custom-Client/2.0")
                .retryConfig(JbhRetryConfig.builder()
                        .maxAttempts(2)
                        .initialDelay(Duration.ofMillis(100))
                        .build())
                .build();
        
        JbhHttpResponse mockResponse = JbhHttpResponse.builder()
                .statusCode(200)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .add("Content-Length", "42")
                        .build())
                .body("{\"message\":\"POST success\",\"data\":\"test\"}")
                .build();
        
        mockAdapter.thenReturn(mockResponse);
        
        JbhHttpClientAdapter adapter = factory.createAdapter(AdapterType.MOCK, customConfig);
        
        try {
            String jsonBody = "{\"message\":\"Hello from JBH Gateway Client\",\"timestamp\":\"" + 
                    java.time.Instant.now() + "\"}";
            
            JbhHttpRequest request = JbhHttpRequest.post("https://httpbin.org/post")
                    .jsonBody(jsonBody)
                    .header("X-Custom-Header", "JBH-Test")
                    .timeout(Duration.ofSeconds(5))
                    .build();
            
            JbhHttpResponse response = adapter.execute(request);
            
            assertTrue(response.isSuccessful());
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getContentLength().isPresent());
            assertEquals(42L, response.getContentLength().get());
            
        } finally {
            adapter.close();
        }
    }

    @Test
    void testAsyncRequests() throws Exception {
        JbhHttpResponse mockResponse = JbhHttpResponse.builder()
                .statusCode(200)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body("{\"async\":\"success\"}")
                .build();
        
        mockAdapter.thenReturn(mockResponse);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest[] requests = {
            JbhHttpRequest.get("https://httpbin.org/delay/1").build(),
            JbhHttpRequest.get("https://httpbin.org/json").build(),
            JbhHttpRequest.get("https://httpbin.org/headers").build()
        };
        
        CompletableFuture<?>[] futures = new CompletableFuture[requests.length];
        
        for (int i = 0; i < requests.length; i++) {
            futures[i] = adapter.executeAsync(requests[i])
                    .thenAccept(response -> {
                        assertTrue(response.isSuccessful());
                        assertEquals(200, response.getStatusCode());
                    });
        }
        
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        
        for (CompletableFuture<?> future : futures) {
            assertTrue(future.isDone());
            assertFalse(future.isCompletedExceptionally());
        }
    }

    @Test
    void testErrorHandling404() {
        JbhHttpResponse errorResponse = JbhHttpResponse.builder()
                .statusCode(404)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body("{\"error\":\"Not Found\"}")
                .build();
        
        mockAdapter.thenReturn(errorResponse);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest request = JbhHttpRequest.get("https://httpbin.org/status/404").build();
        JbhHttpResponse response = adapter.execute(request);
        
        assertFalse(response.isSuccessful());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    void testErrorHandling500() {
        JbhHttpResponse errorResponse = JbhHttpResponse.builder()
                .statusCode(500)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body("{\"error\":\"Internal Server Error\"}")
                .build();
        
        mockAdapter.thenReturn(errorResponse);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest request = JbhHttpRequest.get("https://httpbin.org/status/500").build();
        JbhHttpResponse response = adapter.execute(request);
        
        assertFalse(response.isSuccessful());
        assertEquals(500, response.getStatusCode());
    }

    @Test
    void testErrorHandlingConnectionException() {
        RuntimeException connectionException = new RuntimeException("Connection refused");
        mockAdapter.thenThrow(connectionException);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest request = JbhHttpRequest.get("https://invalid-domain-that-does-not-exist.com").build();
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adapter.execute(request);
        });
        
        assertEquals("Connection refused", exception.getMessage());
    }

    @Test
    void testErrorHandlingTimeout() {
        RuntimeException timeoutException = new RuntimeException("Request timeout");
        mockAdapter.thenThrow(timeoutException);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest timeoutRequest = JbhHttpRequest.get("https://httpbin.org/delay/10")
                .timeout(Duration.ofSeconds(2))
                .build();
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adapter.execute(timeoutRequest);
        });
        
        assertEquals("Request timeout", exception.getMessage());
    }

    @Test
    void testAsyncErrorHandling() throws Exception {
        RuntimeException asyncException = new RuntimeException("Async request failed");
        mockAdapter.thenThrow(asyncException);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest request = JbhHttpRequest.get("https://httpbin.org/error").build();
        
        CompletableFuture<JbhHttpResponse> future = adapter.executeAsync(request);
        
        try {
            future.get(1, TimeUnit.SECONDS);
            fail("Expected CompletionException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("Async request failed", e.getCause().getMessage());
        }
        
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void testTypedResponseDeserialization() {
        String jsonResponse = "{\"id\":\"123\",\"name\":\"John Doe\",\"email\":\"john@example.com\",\"createdAt\":\"2023-01-01T12:00:00\"}";
        
        JbhHttpResponse mockResponse = JbhHttpResponse.builder()
                .statusCode(200)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body(jsonResponse)
                .build();
        
        mockAdapter.thenReturn(mockResponse);
        
        JbhHttpClientAdapter adapter = factory.createAdapter(AdapterType.MOCK);
        
        try {
            JbhHttpRequest request = JbhHttpRequest.get("https://api.example.com/user/123").build();
            
            JbhHttpResponse response = adapter.executeForObject(request, TestUserDto.class);
            
            assertTrue(response.isSuccessful());
            assertEquals(200, response.getStatusCode());
            assertTrue(response.hasTypedBody());
            
            TestUserDto user = response.getBodyAs(TestUserDto.class).orElse(null);
            assertNotNull(user);
            assertEquals("123", user.getId());
            assertEquals("John Doe", user.getName());
            assertEquals("john@example.com", user.getEmail());
            
            // String body should still be available
            assertTrue(response.getBody().isPresent());
            assertEquals(jsonResponse, response.getBody().get());
            
        } finally {
            adapter.close();
        }
    }

    @Test
    void testAsyncTypedResponseDeserialization() throws Exception {
        String jsonResponse = "{\"id\":\"456\",\"name\":\"Jane Smith\",\"email\":\"jane@example.com\",\"createdAt\":\"2023-02-01T10:30:00\"}";
        
        JbhHttpResponse mockResponse = JbhHttpResponse.builder()
                .statusCode(200)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body(jsonResponse)
                .build();
        
        mockAdapter.thenReturn(mockResponse);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest request = JbhHttpRequest.get("https://api.example.com/user/456").build();
        
        CompletableFuture<JbhHttpResponse> future = adapter.executeAsyncForObject(request, TestUserDto.class);
        
        JbhHttpResponse response = future.get(5, TimeUnit.SECONDS);
        
        assertTrue(response.isSuccessful());
        assertTrue(response.hasTypedBody());
        
        TestUserDto user = response.getBodyAs(TestUserDto.class).orElse(null);
        assertNotNull(user);
        assertEquals("456", user.getId());
        assertEquals("Jane Smith", user.getName());
    }

    @Test
    void testTypedResponseWithInvalidJson() {
        String invalidJson = "{invalid json";
        
        JbhHttpResponse mockResponse = JbhHttpResponse.builder()
                .statusCode(200)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body(invalidJson)
                .build();
        
        mockAdapter.thenReturn(mockResponse);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest request = JbhHttpRequest.get("https://api.example.com/user/invalid").build();
        
        JbhHttpResponse response = adapter.executeForObject(request, TestUserDto.class);
        
        assertTrue(response.isSuccessful());
        // Should fallback gracefully - no typed body but string body still available
        assertFalse(response.hasTypedBody());
        assertTrue(response.getBodyAs(TestUserDto.class).isEmpty());
        assertTrue(response.getBody().isPresent());
        assertEquals(invalidJson, response.getBody().get());
    }

    @Test
    void testTypedResponseWithErrorStatus() {
        JbhHttpResponse errorResponse = JbhHttpResponse.builder()
                .statusCode(404)
                .headers(JbhHttpHeaders.builder()
                        .add("Content-Type", "application/json")
                        .build())
                .body("{\"error\":\"User not found\"}")
                .build();
        
        mockAdapter.thenReturn(errorResponse);
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.MOCK);
        
        JbhHttpRequest request = JbhHttpRequest.get("https://api.example.com/user/999").build();
        
        JbhHttpResponse response = adapter.executeForObject(request, TestUserDto.class);
        
        assertFalse(response.isSuccessful());
        assertEquals(404, response.getStatusCode());
        // Should not attempt deserialization for error responses
        assertFalse(response.hasTypedBody());
        assertTrue(response.getBodyAs(TestUserDto.class).isEmpty());
    }
}