## Why

JBH-55 wants one trace per request across all services. Today `jbh-gateway-client` sends no trace context. A call from `jbh-iam` or `jbh-personal-finance` through the gateway starts a new, unrelated trace. This change (JBH-57) makes the client continue the caller's trace. It adds one CLIENT span per call and sends the W3C `traceparent` header.

## What Changes

- New dependency `io.opentelemetry:opentelemetry-api` (version from the OpenTelemetry BOM, property `opentelemetry.version` = `1.62.0`). No SDK, no exporter.
- `JbhGatewayClientBuilder`: new optional method `openTelemetry(OpenTelemetry)` and getter `getOpenTelemetry()`. Default is `OpenTelemetry.noop()`.
- New decorator `TracingHttpClientAdapter`. The adapter chain becomes Tracing, then CircuitBreaker, then Native. One span per logical call (retries are inside it).
- Each domain client declares its path template. It travels in `JbhHttpRequest` as an optional internal field `uriTemplate`.
- `module-info.java`: `requires transitive io.opentelemetry.api;` and `requires io.opentelemetry.context;`.
- `README.md`: new "Tracing" section with a Quarkus and a Spring Boot example. `CLAUDE.md`: Dependencies and Key Patterns lines.
- With the no-op instance, behavior is the same as today.

## Capabilities

### New Capabilities
- `trace-propagation`: one CLIENT span per gateway call, and trace context injection into the request headers.

### Modified Capabilities
None.

## Impact

- Public API: one new optional builder method and getter. Additive. Version stays `1.0.0-SNAPSHOT`.
- Callers that do not pass an `OpenTelemetry` see no change.
- `jbh-iam` and `jbh-personal-finance` (JBH-55 sections 4 and 5) will use the new builder method. They are not part of this change.
- The system map `../jbh-deploy/README.md` needs no edit: no new method, no changed path.
