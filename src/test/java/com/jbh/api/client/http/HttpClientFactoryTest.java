package com.jbh.api.client.http;

import com.jbh.api.client.config.HttpClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating how to test the HTTP client factory
 * and the adapter pattern implementation.
 */
class HttpClientFactoryTest {

    private HttpClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = HttpClientFactory.createDefault();
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.shutdown();
        }
    }

    @Test
    void shouldCreateNativeAdapter() {
        HttpClientAdapter adapter = factory.createAdapter("native");
        
        assertNotNull(adapter);
        assertEquals("native", adapter.getAdapterName());
        assertTrue(adapter.supportsHttp2());
        
        adapter.close();
    }

    @Test
    void shouldCreateAdapterWithCustomConfig() {
        HttpClientConfig config = HttpClientConfig.builder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .enableHttp2(false)
                .build();
        
        HttpClientAdapter adapter = factory.createAdapter("native", config);
        
        assertNotNull(adapter);
        assertEquals("native", adapter.getAdapterName());
        
        adapter.close();
    }

    @Test
    void shouldCacheAdapters() {
        HttpClientAdapter adapter1 = factory.getOrCreateAdapter("native");
        HttpClientAdapter adapter2 = factory.getOrCreateAdapter("native");
        
        // Should return the same cached instance
        assertSame(adapter1, adapter2);
    }

    @Test
    void shouldCreateDifferentAdaptersForDifferentConfigs() {
        HttpClientConfig config1 = HttpClientConfig.builder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
        HttpClientConfig config2 = HttpClientConfig.builder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
        
        HttpClientAdapter adapter1 = factory.getOrCreateAdapter("native", config1);
        HttpClientAdapter adapter2 = factory.getOrCreateAdapter("native", config2);
        
        // Should create different instances for different configs
        assertNotSame(adapter1, adapter2);
    }

    @Test
    void shouldRegisterCustomAdapter() {
        factory.registerAdapter("mock", config -> new MockHttpClientAdapter());
        
        assertTrue(factory.getSupportedAdapterTypes().contains("mock"));
        
        HttpClientAdapter adapter = factory.createAdapter("mock");
        assertEquals("mock", adapter.getAdapterName());
        
        adapter.close();
    }

    @Test
    void shouldThrowExceptionForUnsupportedAdapter() {
        assertThrows(IllegalArgumentException.class, 
                () -> factory.createAdapter("unsupported"));
    }

    @Test
    void shouldEvictFromCache() {
        HttpClientConfig config = HttpClientConfig.defaultConfig();
        HttpClientAdapter adapter = factory.getOrCreateAdapter("native", config);
        
        assertNotNull(adapter);
        
        // Evict from cache
        factory.evictFromCache("native", config);
        
        // Should create a new instance
        HttpClientAdapter newAdapter = factory.getOrCreateAdapter("native", config);
        assertNotSame(adapter, newAdapter);
        
        newAdapter.close();
    }

    @Test
    void shouldReturnSupportedAdapterTypes() {
        var supportedTypes = factory.getSupportedAdapterTypes();
        
        assertFalse(supportedTypes.isEmpty());
        assertTrue(supportedTypes.contains("native"));
    }
}