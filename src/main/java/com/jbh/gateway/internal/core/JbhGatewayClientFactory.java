package com.jbh.gateway.internal.core;

import com.jbh.gateway.client.JbhGatewayClient;
import com.jbh.gateway.client.users.JbhUserApiGatewayClient;
import com.jbh.gateway.internal.domains.users.JbhUserApiGatewayClientImpl;

/**
 * Factory for creating JBH API client instances.
 * 
 * This factory provides the entry point for microservices to obtain
 * client instances without needing to know about implementation details.
 * All implementation classes are hidden within the module.
 */
public final class JbhGatewayClientFactory {

    private JbhGatewayClientFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a new user API gateway client with the specified host configuration.
     *
     * @param hostConfig the gateway host configuration
     * @return a new user API gateway client instance
     * @throws IllegalArgumentException if hostConfig is null
     */
    public static JbhUserApiGatewayClient createUserApiClient(JbhGatewayClient hostConfig) {
        if (hostConfig == null) {
            throw new IllegalArgumentException("Host configuration cannot be null");
        }
        return new JbhUserApiGatewayClientImpl(hostConfig);
    }
}
