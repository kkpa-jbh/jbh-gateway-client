package com.jbh.api.client.http;

import com.jbh.api.client.config.HttpClientConfig;
import com.jbh.api.client.http.adapter.NativeHttpClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Factory for creating and managing HTTP client adapter instances.
 * Supports different HTTP client implementations and provides caching for efficient resource management.
 * Allows easy switching between different HTTP client implementations for testing and production use.
 */
public class HttpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);

    private final Map<String, Function<HttpClientConfig, HttpClientAdapter>> adapterFactories;
    private final Map<String, HttpClientAdapter> adapterCache;
    private final HttpClientConfig defaultConfig;

    public HttpClientFactory() {
        this(HttpClientConfig.defaultConfig());
    }

    public HttpClientFactory(HttpClientConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
        this.adapterFactories = new ConcurrentHashMap<>();
        this.adapterCache = new ConcurrentHashMap<>();
        
        registerDefaultAdapters();
    }

    /**
     * Create an HTTP client adapter with the default configuration.
     * 
     * @param adapterType the type of adapter to create (e.g., "native", "okhttp")
     * @return the HTTP client adapter instance
     * @throws IllegalArgumentException if the adapter type is not supported
     */
    public HttpClientAdapter createAdapter(String adapterType) {
        return createAdapter(adapterType, defaultConfig);
    }

    /**
     * Create an HTTP client adapter with the specified configuration.
     * 
     * @param adapterType the type of adapter to create
     * @param config the configuration to use
     * @return the HTTP client adapter instance
     * @throws IllegalArgumentException if the adapter type is not supported
     */
    public HttpClientAdapter createAdapter(String adapterType, HttpClientConfig config) {
        Function<HttpClientConfig, HttpClientAdapter> factory = adapterFactories.get(adapterType.toLowerCase());
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported HTTP client adapter type: " + adapterType + 
                    ". Supported types: " + getSupportedAdapterTypes());
        }

        HttpClientAdapter adapter = factory.apply(config);
        log.info("Created {} HTTP client adapter with config: {}", adapterType, config);
        return adapter;
    }

    /**
     * Get or create a cached HTTP client adapter with the default configuration.
     * This method provides efficient resource usage by reusing adapter instances.
     * 
     * @param adapterType the type of adapter to get or create
     * @return the cached HTTP client adapter instance
     */
    public HttpClientAdapter getOrCreateAdapter(String adapterType) {
        return getOrCreateAdapter(adapterType, defaultConfig);
    }

    /**
     * Get or create a cached HTTP client adapter with the specified configuration.
     * The cache key is based on both adapter type and configuration.
     * 
     * @param adapterType the type of adapter to get or create
     * @param config the configuration to use
     * @return the cached HTTP client adapter instance
     */
    public HttpClientAdapter getOrCreateAdapter(String adapterType, HttpClientConfig config) {
        String cacheKey = adapterType.toLowerCase() + "_" + config.hashCode();
        
        return adapterCache.computeIfAbsent(cacheKey, key -> {
            log.debug("Creating new cached adapter for key: {}", key);
            return createAdapter(adapterType, config);
        });
    }

    /**
     * Register a custom HTTP client adapter factory.
     * This allows for extending the factory with new adapter implementations.
     * 
     * @param adapterType the type name for the adapter
     * @param factory the factory function that creates adapter instances
     */
    public void registerAdapter(String adapterType, Function<HttpClientConfig, HttpClientAdapter> factory) {
        adapterFactories.put(adapterType.toLowerCase(), factory);
        log.info("Registered HTTP client adapter factory for type: {}", adapterType);
    }

    /**
     * Get the list of supported adapter types.
     * 
     * @return set of supported adapter type names
     */
    public java.util.Set<String> getSupportedAdapterTypes() {
        return adapterFactories.keySet();
    }

    /**
     * Clear the adapter cache and close all cached adapters.
     * This should be called during application shutdown.
     */
    public void shutdown() {
        log.info("Shutting down HTTP client factory, closing {} cached adapters", adapterCache.size());
        
        adapterCache.values().forEach(adapter -> {
            try {
                adapter.close();
            } catch (Exception e) {
                log.warn("Error closing HTTP client adapter: {}", e.getMessage(), e);
            }
        });
        
        adapterCache.clear();
    }

    /**
     * Remove a specific adapter from the cache and close it.
     * 
     * @param adapterType the type of adapter to remove
     * @param config the configuration used for the adapter
     */
    public void evictFromCache(String adapterType, HttpClientConfig config) {
        String cacheKey = adapterType.toLowerCase() + "_" + config.hashCode();
        HttpClientAdapter adapter = adapterCache.remove(cacheKey);
        
        if (adapter != null) {
            log.debug("Evicted adapter from cache: {}", cacheKey);
            try {
                adapter.close();
            } catch (Exception e) {
                log.warn("Error closing evicted HTTP client adapter: {}", e.getMessage(), e);
            }
        }
    }

    private void registerDefaultAdapters() {
        // Register Native Java HTTP Client adapter
        registerAdapter("native", NativeHttpClientAdapter::new);
        
        // Register OkHttp adapter if available on classpath
        try {
            Class.forName("okhttp3.OkHttpClient");
            log.debug("OkHttp found on classpath, but adapter not implemented in this example");
            // In a real implementation, you would register the OkHttp adapter here:
            // registerAdapter("okhttp", OkHttpClientAdapter::new);
        } catch (ClassNotFoundException e) {
            log.debug("OkHttp not found on classpath, skipping registration");
        }
        
        log.info("Registered default HTTP client adapters: {}", getSupportedAdapterTypes());
    }

    /**
     * Create a default factory instance with standard configuration.
     * 
     * @return a new factory instance with default settings
     */
    public static HttpClientFactory createDefault() {
        return new HttpClientFactory();
    }

    /**
     * Create a factory instance with custom default configuration.
     * 
     * @param defaultConfig the default configuration to use
     * @return a new factory instance with the specified default configuration
     */
    public static HttpClientFactory createWithConfig(HttpClientConfig defaultConfig) {
        return new HttpClientFactory(defaultConfig);
    }
}