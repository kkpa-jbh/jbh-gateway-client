/**
 * JBH Gateway Client Module
 * 
 * This module provides a high-level HTTP client library for microservices 
 * to communicate with the JBH Gateway. It exposes only the necessary public
 * APIs while hiding all implementation details.
 * 
 * Public API:
 * - com.jbh.gateway.client: Core API types and exceptions
 * - com.jbh.gateway.client;.users: User domain client interface
 * 
 * All implementation details (core, http, config) are encapsulated and hidden.
 */
module com.jbh.gateway {
    // External dependencies
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.module.paramnames;
    requires org.slf4j;
    
    // Public API exports - what microservices can use
    exports com.jbh.gateway.client;
    exports com.jbh.gateway.client.users;

}