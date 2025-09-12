package com.jbh.api.client;

import com.jbh.api.client.config.JbhHttpClientConfig;
import com.jbh.api.client.config.JbhRetryConfig;
import com.jbh.api.client.http.JbhHttpClientAdapter;
import com.jbh.api.client.http.JbhHttpClientFactory;
import com.jbh.api.client.http.model.JbhHttpRequest;
import com.jbh.api.client.http.model.JbhHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstration of the HTTP client adapter pattern.
 * Shows how different HTTP client implementations can be used interchangeably
 * through the adapter pattern, enabling easy testing and configuration switching.
 */
public class JbhApiGatewayClientRunner {
    
    private static final Logger log = LoggerFactory.getLogger(JbhApiGatewayClientRunner.class);

    public static void main(String[] args) {
        log.info("Starting JBH Gateway Client demonstration...");
        
        try {
            // Create HTTP client factory
            JbhHttpClientFactory factory = JbhHttpClientFactory.createDefault();
            
            // Demonstrate different configurations
            demonstrateBasicUsage(factory);
            demonstrateCustomConfiguration(factory);
            demonstrateAsyncRequests(factory);
            demonstrateErrorHandling(factory);
            
            // Shutdown factory to clean up resources
            factory.shutdown();
            
        } catch (Exception e) {
            log.error("Error during demonstration", e);
        }
        
        log.info("JBH Gateway Client demonstration completed.");
    }

    private static void demonstrateBasicUsage(JbhHttpClientFactory factory) {
        log.info("=== Demonstrating Basic HTTP Client Adapter Usage ===");
        
        // Create adapter with default configuration
        JbhHttpClientAdapter adapter = factory.createAdapter("native");
        
        try {
            // Create a simple GET request
            JbhHttpRequest request = JbhHttpRequest.get("https://httpbin.org/get")
                    .header("User-Agent", "jbh-api-client/1.0")
                    .header("Accept", "application/json")
                    .build();
            
            log.info("Executing request: {} {}", request.getMethod(), request.getUri());
            
            // Execute the request
            JbhHttpResponse response = adapter.execute(request);
            
            log.info("Response: {} - {}", response.getStatusCode(), 
                    response.isSuccessful() ? "SUCCESS" : "ERROR");
            log.info("Content-Type: {}", response.getContentType().orElse("N/A"));
            log.info("Response body length: {} characters", 
                    response.getBody().map(String::length).orElse(0));
            
        } catch (Exception e) {
            log.warn("Request failed (this is expected if no internet connection): {}", e.getMessage());
        } finally {
            adapter.close();
        }
    }

    private static void demonstrateCustomConfiguration(JbhHttpClientFactory factory) {
        log.info("=== Demonstrating Custom Configuration ===");
        
        // Create custom configuration
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
        
        log.info("Created custom configuration: {}", customConfig);
        
        // Create adapter with custom configuration
        JbhHttpClientAdapter adapter = factory.createAdapter("native", customConfig);
        
        try {
            // Test POST request with JSON body
            String jsonBody = "{\"message\":\"Hello from JBH Gateway Client\",\"timestamp\":\"" + 
                    java.time.Instant.now() + "\"}";
            
            JbhHttpRequest request = JbhHttpRequest.post("https://httpbin.org/post")
                    .jsonBody(jsonBody)
                    .header("X-Custom-Header", "JBH-Test")
                    .timeout(Duration.ofSeconds(5))
                    .build();
            
            log.info("Executing POST request with JSON body");
            
            JbhHttpResponse response = adapter.execute(request);
            
            log.info("POST Response: {} - Content-Length: {}", 
                    response.getStatusCode(),
                    response.getContentLength().orElse(-1L));
            
        } catch (Exception e) {
            log.warn("POST request failed (this is expected if no internet connection): {}", e.getMessage());
        } finally {
            adapter.close();
        }
    }

    private static void demonstrateAsyncRequests(JbhHttpClientFactory factory) {
        log.info("=== Demonstrating Asynchronous Requests ===");
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter("native");
        
        try {
            // Create multiple requests
            JbhHttpRequest[] requests = {
                JbhHttpRequest.get("https://httpbin.org/delay/1").build(),
                JbhHttpRequest.get("https://httpbin.org/json").build(),
                JbhHttpRequest.get("https://httpbin.org/headers").build()
            };
            
            // Execute all requests asynchronously
            CompletableFuture<?>[] futures = new CompletableFuture[requests.length];
            
            for (int i = 0; i < requests.length; i++) {
                final int requestIndex = i;
                futures[i] = adapter.executeAsync(requests[i])
                        .thenAccept(response -> {
                            log.info("Async request {} completed: {} - {}", 
                                    requestIndex, response.getStatusCode(),
                                    response.isSuccessful() ? "SUCCESS" : "ERROR");
                        })
                        .exceptionally(throwable -> {
                            log.warn("Async request {} failed: {}", requestIndex, throwable.getMessage());
                            return null;
                        });
            }
            
            // Wait for all requests to complete
            CompletableFuture.allOf(futures).join();
            
            log.info("All async requests completed");
            
        } catch (Exception e) {
            log.warn("Async requests failed: {}", e.getMessage());
        }
    }

    private static void demonstrateErrorHandling(JbhHttpClientFactory factory) {
        log.info("=== Demonstrating Error Handling ===");
        
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter("native");
        
        // Test various error scenarios
        testErrorScenario(adapter, "https://httpbin.org/status/404", "404 Not Found");
        testErrorScenario(adapter, "https://httpbin.org/status/500", "500 Server Error");
        testErrorScenario(adapter, "https://invalid-domain-that-does-not-exist.com", "Connection Error");
        
        // Test timeout
        JbhHttpRequest timeoutRequest = JbhHttpRequest.get("https://httpbin.org/delay/10")
                .timeout(Duration.ofSeconds(2))
                .build();
        testErrorScenario(adapter, timeoutRequest, "Timeout Error");
    }

    private static void testErrorScenario(JbhHttpClientAdapter adapter, String url, String scenarioName) {
        testErrorScenario(adapter, JbhHttpRequest.get(url).build(), scenarioName);
    }

    private static void testErrorScenario(JbhHttpClientAdapter adapter, JbhHttpRequest request, String scenarioName) {
        try {
            log.info("Testing {}: {} {}", scenarioName, request.getMethod(), request.getUri());
            JbhHttpResponse response = adapter.execute(request);
            
            if (response.isSuccessful()) {
                log.info("{} - Unexpected success: {}", scenarioName, response.getStatusCode());
            } else {
                log.info("{} - Expected error response: {}", scenarioName, response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.info("{} - Caught expected exception: {} - {}", 
                    scenarioName, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
