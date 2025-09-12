package com.jbh.api.client.http.model;

import java.util.*;

/**
 * Immutable representation of HTTP headers.
 * Provides case-insensitive header operations and support for multiple values per header.
 * Designed to be thread-safe and efficient for header manipulation across different HTTP clients.
 */
public final class JbhHttpHeaders {

    private final Map<String, List<String>> headers;

    private JbhHttpHeaders(Map<String, List<String>> headers) {
        this.headers = Collections.unmodifiableMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER) {{
            headers.forEach((key, values) -> put(key, List.copyOf(values)));
        }});
    }

    public static JbhHttpHeaders fromMap(Map<String, String> headers) {
        Map<String, List<String>> headerMap = new HashMap<>();
        headers.forEach((key, value) -> headerMap.put(key, List.of(value)));
        return new JbhHttpHeaders(headerMap);
    }

    public static JbhHttpHeaders fromMultiMap(Map<String, List<String>> headers) {
        return new JbhHttpHeaders(headers);
    }

    public static JbhHttpHeaders empty() {
        return new JbhHttpHeaders(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getFirst(String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    public List<String> getAll(String name) {
        return headers.getOrDefault(name, List.of());
    }

    public Set<String> getNames() {
        return headers.keySet();
    }

    public Map<String, List<String>> asMap() {
        return headers;
    }

    public boolean contains(String name) {
        return headers.containsKey(name);
    }

    public JbhHttpHeaders with(String name, String value) {
        return builder()
                .addAll(this)
                .add(name, value)
                .build();
    }

    public JbhHttpHeaders without(String name) {
        Map<String, List<String>> newHeaders = new HashMap<>(headers);
        newHeaders.remove(name);
        return new JbhHttpHeaders(newHeaders);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JbhHttpHeaders that = (JbhHttpHeaders) o;
        return Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers);
    }

    @Override
    public String toString() {
        return "HttpHeaders{" + headers + '}';
    }

    public static class Builder {
        private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public Builder add(String name, String value) {
            headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            return this;
        }

        public Builder set(String name, String value) {
            headers.put(name, new ArrayList<>(List.of(value)));
            return this;
        }

        public Builder addAll(JbhHttpHeaders httpHeaders) {
            httpHeaders.headers.forEach((name, values) -> 
                headers.computeIfAbsent(name, k -> new ArrayList<>()).addAll(values));
            return this;
        }

        public Builder addAll(Map<String, String> headerMap) {
            headerMap.forEach(this::add);
            return this;
        }

        public JbhHttpHeaders build() {
            return new JbhHttpHeaders(headers);
        }
    }
}