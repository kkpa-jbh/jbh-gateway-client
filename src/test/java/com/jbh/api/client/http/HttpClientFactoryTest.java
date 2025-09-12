package com.jbh.api.client.http;

import com.jbh.api.client.config.JbhHttpClientConfig;
import com.jbh.api.client.core.http.AdapterType;
import com.jbh.api.client.core.http.JbhHttpClientAdapter;
import com.jbh.api.client.core.http.JbhHttpClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating how to test the HTTP client factory
 * and the adapter pattern implementation.
 */
class HttpClientFactoryTest {

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
    void shouldCreateNativeAdapter() {
        JbhHttpClientAdapter adapter = factory.createAdapter(AdapterType.NATIVE);
        
        assertNotNull(adapter);
        assertEquals("native", adapter.getAdapterName());
        assertTrue(adapter.supportsHttp2());
        
        adapter.close();
    }

    @Test
    void shouldCreateAdapterWithCustomConfig() {
        JbhHttpClientConfig config = JbhHttpClientConfig.builder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .enableHttp2(false)
                .build();
        
        JbhHttpClientAdapter adapter = factory.createAdapter(AdapterType.NATIVE, config);
        
        assertNotNull(adapter);
        assertEquals("native", adapter.getAdapterName());
        
        adapter.close();
    }

    @Test
    void shouldCacheAdapters() {
        JbhHttpClientAdapter adapter1 = factory.getOrCreateAdapter(AdapterType.NATIVE);
        JbhHttpClientAdapter adapter2 = factory.getOrCreateAdapter(AdapterType.NATIVE);
        
        // Should return the same cached instance
        assertSame(adapter1, adapter2);
    }

    @Test
    void shouldCreateDifferentAdaptersForDifferentConfigs() {
        JbhHttpClientConfig config1 = JbhHttpClientConfig.builder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
        JbhHttpClientConfig config2 = JbhHttpClientConfig.builder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
        
        JbhHttpClientAdapter adapter1 = factory.getOrCreateAdapter(AdapterType.NATIVE, config1);
        JbhHttpClientAdapter adapter2 = factory.getOrCreateAdapter(AdapterType.NATIVE, config2);
        
        // Should create different instances for different configs
        assertNotSame(adapter1, adapter2);
    }

    @Test
    void shouldRegisterCustomAdapter() {
        factory.registerAdapter(AdapterType.MOCK, config -> new MockHttpClientAdapter());
        
        assertTrue(factory.getSupportedAdapterTypes().contains(AdapterType.MOCK));
        
        JbhHttpClientAdapter adapter = factory.createAdapter(AdapterType.MOCK);
        assertEquals("mock", adapter.getAdapterName());
        
        adapter.close();
    }

    @Test
    void shouldThrowExceptionForUnsupportedAdapterString() {
        assertThrows(IllegalArgumentException.class, 
                () -> AdapterType.fromValue("unsupported"));
    }

    @Test
    void shouldEvictFromCache() {
        JbhHttpClientConfig config = JbhHttpClientConfig.defaultConfig();
        JbhHttpClientAdapter adapter = factory.getOrCreateAdapter(AdapterType.NATIVE, config);
        
        assertNotNull(adapter);
        
        // Evict from cache
        factory.evictFromCache(AdapterType.NATIVE, config);
        
        // Should create a new instance
        JbhHttpClientAdapter newAdapter = factory.getOrCreateAdapter(AdapterType.NATIVE, config);
        assertNotSame(adapter, newAdapter);
        
        newAdapter.close();
    }

    @Test
    void shouldReturnSupportedAdapterTypes() {
        var supportedTypes = factory.getSupportedAdapterTypes();
        
        assertFalse(supportedTypes.isEmpty());
        assertTrue(supportedTypes.contains(AdapterType.NATIVE));
    }
}