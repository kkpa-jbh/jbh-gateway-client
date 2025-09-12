package com.jbh.api.client.integration;

import com.jbh.api.client.config.JbhHttpClientConfig;
import com.jbh.api.client.config.JbhRetryConfig;
import com.jbh.api.client.core.http.JbhHttpClientAdapter;
import com.jbh.api.client.core.http.JbhHttpClientFactory;
import com.jbh.api.client.core.http.model.JbhHttpRequest;
import com.jbh.api.client.core.http.model.JbhHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class JbhApiGatewayClientIntegrationTest {

    private JbhHttpClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = JbhHttpClientFactory.createDefault();
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.shutdown();
        }
    }

    @Test
    void testBasicGetRequest() {
        JbhHttpClientAdapter adapter = factory.createAdapter("native");
        
        try {
            JbhHttpRequest request = JbhHttpRequest.get("https://httpbin.org/get")
                    .header("User-Agent", "jbh-api-client/1.0")
                    .header("Accept", "application/json")
                    .build();
            
            JbhHttpResponse response = adapter.execute(request);
            
            assertTrue(response.isSuccessful(), "Request should be successful");
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getContentType().isPresent());
            assertTrue(response.getContentType().get().contains("application/json"));
            assertTrue(response.getBody().isPresent());
            
            String body = response.getBody().get();
            assertTrue(body.contains("User-Agent"), "Response should contain User-Agent header");
            assertTrue(body.contains("jbh-api-client/1.0"), "Response should contain our custom User-Agent");
            
        } catch (Exception e) {
            fail("Basic GET request should not fail: " + e.getMessage());
        } finally {
            adapter.close();
        }
    }

    @Test
    void testCustomConfigurationWithPost() {
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
        
        JbhHttpClientAdapter adapter = factory.createAdapter("native", customConfig);
        
        try {
            String jsonBody = "{\"message\":\"Hello from JBH Gateway Client\",\"timestamp\":\"" + 
                    java.time.Instant.now() + "\"}";
            
            JbhHttpRequest request = JbhHttpRequest.post("https://httpbin.org/post")
                    .jsonBody(jsonBody)
                    .header("X-Custom-Header", "JBH-Test")
                    .timeout(Duration.ofSeconds(5))
                    .build();
            
            JbhHttpResponse response = adapter.execute(request);
            
            assertTrue(response.isSuccessful(), "POST request should be successful");
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getContentLength().isPresent());
            assertTrue(response.getContentLength().get() > 0);
            
            String responseBody = response.getBody().orElse("");
            assertTrue(responseBody.contains("Hello from JBH Gateway Client"), 
                    "Response should contain our JSON message");
            assertTrue(responseBody.contains("JBH-Test"), 
                    "Response should contain our custom header");
            
        } catch (Exception e) {
            fail("POST request with custom config should not fail: " + e.getMessage());
        } finally {
            adapter.close();
        }
    }

    @Test
    void testAsynchronousRequests() throws Exception {
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter("native");
        
        JbhHttpRequest[] requests = {
            JbhHttpRequest.get("https://httpbin.org/delay/1").build(),
            JbhHttpRequest.get("https://httpbin.org/json").build(),
            JbhHttpRequest.get("https://httpbin.org/headers").build()
        };
        
        CompletableFuture<JbhHttpResponse>[] futures = new CompletableFuture[requests.length];
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < requests.length; i++) {
            futures[i] = adapter.executeAsync(requests[i]);
        }
        
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Should be significantly less than 20 seconds (1 + 1 + 1) due to parallel execution
        assertTrue(totalTime < 20000,
                "Parallel execution should be faster than sequential: " + totalTime + "ms");
        
        for (int i = 0; i < futures.length; i++) {
            assertTrue(futures[i].isDone(), "Future " + i + " should be completed");
            assertFalse(futures[i].isCompletedExceptionally(), "Future " + i + " should not have failed");
            
            JbhHttpResponse response = futures[i].get();
            assertTrue(response.isSuccessful(), "Async request " + i + " should be successful");
            assertEquals(200, response.getStatusCode());
        }
    }

    @Test
    void testErrorHandling404() {
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter("native");
        
        JbhHttpRequest request = JbhHttpRequest.get("https://httpbin.org/status/404").build();
        
        try {
            JbhHttpResponse response = adapter.execute(request);
            
            assertFalse(response.isSuccessful(), "404 response should not be successful");
            assertEquals(404, response.getStatusCode());
            
        } catch (Exception e) {
            fail("404 should return response, not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testErrorHandling500() {
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter("native");
        
        JbhHttpRequest request = JbhHttpRequest.get("https://httpbin.org/status/500").build();
        
        try {
            JbhHttpResponse response = adapter.execute(request);
            
            assertFalse(response.isSuccessful(), "500 response should not be successful");
            assertEquals(500, response.getStatusCode());
            
        } catch (Exception e) {
            fail("500 should return response, not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testConnectionError() {
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter("native");
        
        JbhHttpRequest request = JbhHttpRequest.get("https://invalid-domain-that-does-not-exist-12345.com")
                .timeout(Duration.ofSeconds(5))
                .build();
        
        assertThrows(Exception.class, () -> {
            adapter.execute(request);
        }, "Connection to invalid domain should throw exception");
    }

    @Test
    void testTimeoutError() {
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter("native");
        
        JbhHttpRequest timeoutRequest = JbhHttpRequest.get("https://httpbin.org/delay/10")
                .timeout(Duration.ofSeconds(2))
                .build();
        
        assertThrows(Exception.class, () -> {
            adapter.execute(timeoutRequest);
        }, "Request with short timeout should throw exception");
    }

    @Test
    void testAsyncErrorHandling() throws Exception {
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter("native");
        
        JbhHttpRequest request = JbhHttpRequest.get("https://invalid-domain-async-test-12345.com")
                .timeout(Duration.ofSeconds(3))
                .build();
        
        CompletableFuture<JbhHttpResponse> future = adapter.executeAsync(request);
        
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Async request to invalid domain should fail");
        } catch (Exception e) {
            assertTrue(future.isCompletedExceptionally(), "Future should be completed exceptionally");
            assertNotNull(e.getCause(), "Exception should have a cause");
        }
    }


}