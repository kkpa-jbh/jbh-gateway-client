package com.jbh.gateway.internal.core.http;

/**
 * Enum representing the different types of HTTP client adapters available.
 * This provides type safety and prevents invalid adapter type strings from being used.
 */
public enum AdapterType {
    
    /**
     * Native Java HTTP client adapter (uses Java 11+ HttpClient).
     */
    NATIVE("native"),
    
    /**
     * Mock adapter for testing purposes.
     */
    MOCK("mock");
    
    private final String value;
    
    AdapterType(String value) {
        this.value = value;
    }
    
    /**
     * Gets the string value of this adapter type.
     * 
     * @return the string representation of this adapter type
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Converts a string value to an AdapterType enum.
     * 
     * @param value the string value to convert
     * @return the corresponding AdapterType
     * @throws IllegalArgumentException if the value doesn't match any adapter type
     */
    public static AdapterType fromValue(String value) {
        for (AdapterType type : AdapterType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown adapter type: " + value);
    }
    
    /**
     * Returns the string representation of this adapter type.
     * 
     * @return the string value
     */
    @Override
    public String toString() {
        return value;
    }
}