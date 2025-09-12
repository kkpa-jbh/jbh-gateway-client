package com.jbh.gateway.internal.core.http.model;

/**
 * Enumeration of supported HTTP methods.
 * Provides type-safe representation of HTTP methods across all client implementations.
 */
public enum JbhHttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS
}