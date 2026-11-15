# JBH Gateway Client

A Java 21 HTTP gateway client library providing type-safe, domain-specific clients for JBH microservices.

## Quick Start On the Caller of this Gateway Client

```java
import com.jbh.gateway.client.JbhGatewayClientBuilder;
import com.jbh.gateway.client.JbhGatewayException;
import com.jbh.gateway.client.JbhHttpResponse;

var builder = JbhGatewayClientBuilder.builder()
    .baseUrl("https://gateway.jbh.com")
    .sourceService("my-service")
    .build();

var userClient = builder.getUserClient();
var response = userClient.findUserId(Map.of(
    "Authorization", "Bearer " + token
));

if (response.isSuccessful()) {
    UUID userId = response.getBodyAs(UUID.class).orElseThrow();
}
```

Client methods:

| Client | Method | Gateway call | Used by |
|--------|--------|--------------|---------|
| `getUserClient()` | `findUserId(headers)` | `GET /jbh-api/users/find-user-id` | `jbh-personal-finance` |
| `getUserClient()` | `findUserGroup(groupId, headers)` | `GET /jbh-api/user-groups/{groupId}`. `200` member, `403` not a member, `404` no group | `jbh-personal-finance` (team preferences) |
| `getNotificationClient()` | `sendNotification(...)` | `POST /jbh-api/notifications/v1` | `jbh-iam` |

---

## Architecture Overview

```
src/main/java/com/jbh/gateway/
├── client/                          # PUBLIC API (exported via module-info)
│   ├── JbhGatewayClientBuilder.java       # Entry point - creates all clients
│   ├── JbhGatewayException.java           # Standard checked exception
│   ├── JbhHttpResponse.java               # Standard response wrapper
│   ├── users/                             # User domain
│   │   └── JbhUserGatewayClient.java
│   └── notifications/                     # Notification domain
│       ├── JbhNotificationGatewayClient.java
│       ├── NotificationType.java
│       └── SendNotificationCommand.java
│
└── internal/                        # HIDDEN IMPLEMENTATION (not exported)
    ├── domains/
    │   ├── BaseApiGatewayClient.java      # Base class for all clients
    │   ├── users/
    │   │   └── JbhUserGatewayClientImpl.java
    │   └── notifications/
    │       └── JbhNotificationGatewayClientImpl.java
    └── core/                              # HTTP infrastructure
        └── http/
            ├── JbhHttpClientAdapter.java      # Transport seam: execute(request). Tests swap it.
            └── adapter/
                ├── NativeHttpClientAdapter.java   # java.net.http + retries
                └── TracingHttpClientAdapter.java  # One CLIENT span per call + traceparent (added in wave 2)

src/test/java/com/jbh/gateway/
├── client/                          # CLIENT TESTS (simulates library callers)
│   ├── users/
│   │   └── JbhUserGatewayClientTest.java
│   └── notifications/
│       └── JbhNotificationGatewayClientTest.java
│
└── internal/                        # INTERNAL TESTS (cross-client, infrastructure)
    └── integration/
        └── JbhGatewayClientIntegrationTest.java
```

---

## Developer Guide: Adding New Client Methods

### Standards

Every client method **MUST**:
1. Return `JbhHttpResponse` (provides status, headers, typed body)
2. Throw `JbhGatewayException` (checked exception with error code and HTTP status)
3. Accept `Map<String, String>` for headers (converted internally to `JbhHttpHeaders`)

### Step-by-Step: Adding a New Method

#### 1. Define Interface Method

Location: `src/main/java/com/jbh/gateway/client/<domain>/`

```java
// In JbhXxxGatewayClient.java
public interface JbhXxxGatewayClient {
    JbhHttpResponse myMethod(Map<String, String> metadata) throws JbhGatewayException;

    // For POST/PUT with body, add request DTO parameter:
    JbhHttpResponse createSomething(MyRequest request, Map<String, String> metadata)
        throws JbhGatewayException;
}
```

#### 2. Create Implementation

Location: `src/main/java/com/jbh/gateway/internal/domains/<domain>/`

```java
public class JbhXxxGatewayClientImpl extends BaseApiGatewayClient
    implements JbhXxxGatewayClient {

    private static final String MY_PATH = "/xxx/my-endpoint";
    // Path template for tracing: no ids, no query string.
    private static final String MY_TEMPLATE = "/xxx/my-endpoint";

    public JbhXxxGatewayClientImpl(JbhGatewayClientBuilder config)
        throws JbhGatewayException {
        super(config);
    }

    @Override
    public JbhHttpResponse myMethod(Map<String, String> clientHttpHeaders)
        throws JbhGatewayException {
        JbhHttpHeaders headers = JbhHttpHeaders.fromMap(clientHttpHeaders);
        validator.validateAuthorizationHeader(headers);
        return executeGet(MY_PATH, MY_TEMPLATE, headers);
    }

    @Override
    public JbhHttpResponse createSomething(MyRequest request, Map<String, String> clientHttpHeaders)
        throws JbhGatewayException {
        JbhHttpHeaders headers = JbhHttpHeaders.fromMap(clientHttpHeaders);
        validator.validateAuthorizationHeader(headers);
        String jsonBody = JacksonJsonUtil.toJson(request);
        return executePost(MY_PATH, MY_TEMPLATE, jsonBody, headers);
    }
}
```

Always pass the path template (the second argument). The span name and `url.template` come from it.
Put the variable part in braces, for example `/xxx/{id}`, and never add a query string. The old
overloads without a template still work, but their span name has only the HTTP method.

#### 3. Add Factory Method to Builder

Location: `src/main/java/com/jbh/gateway/client/JbhGatewayClientBuilder.java`

```java
public JbhXxxGatewayClient getXxxClient() throws JbhGatewayException {
    return new JbhXxxGatewayClientImpl(this);
}
```

#### 4. (Optional) Create Request/Response DTOs

Location: `src/main/java/com/jbh/gateway/client/<domain>/`

```java
// Request DTO - use record for immutability
public record MyRequest(
    UUID id,
    String name,
    Map<String, Object> metadata
) {}
```

---

## Install and publish

- **Local:** `mvn install`. It puts the library in `~/.m2`. `jbh-iam` and `jbh-personal-finance` find it there.
  Install `jbh-notification-contracts` first (from `jbh-notification`).
- **CI:** `.github/workflows/publish.yml` runs `mvn -Pgithub deploy` on every push to `main`.
  It publishes to GitHub Packages (`https://maven.pkg.github.com/kkpa-jbh/jbh-gateway-client`).
  The `github` profile reads `jbh-notification-contracts` from the `jbh-notification` package.
- **CI order:** contracts (`jbh-notification` → `publish`) → this library → `jbh-iam` and `jbh-personal-finance` images.

## BaseApiGatewayClient Methods

The base class provides these protected methods:

| Method | Description |
|--------|-------------|
| `executeGet(path, uriTemplate, headers)` | GET request to `/jbh-api{path}`. The template names the span. Use these overloads. |
| `executeGet(path, headers)` | GET request to `/jbh-api{path}`, no template |
| `executeGet(path, headers, type)` | GET request; a 2xx JSON body is also read as `type` (`getBodyAs(type)`) |
| `executePost(path, uriTemplate, body, headers)` | POST request with JSON body |
| `executePut(path, uriTemplate, body, headers)` | PUT request with JSON body |
| `executeDelete(path, uriTemplate, headers)` | DELETE request |

Each of `executePost`, `executePut` and `executeDelete` also has an older overload without `uriTemplate`.

All paths are automatically prefixed with `/jbh-api`.
All methods throw `JbhGatewayException` when the call gets no HTTP response (see "Exception Handling").

---

## JbhHttpResponse Usage

```java
JbhHttpResponse response = client.someMethod(headers);

// Status checks
response.isSuccessful();  // 200-299
response.isClientError(); // 400-499
response.isServerError(); // 500+

// Get typed body (deserialized from JSON)
Optional<UUID> uuid = response.getBodyAs(UUID.class);
Optional<MyDto> dto = response.getBodyAs(MyDto.class);

// Get raw body
Optional<String> rawJson = response.getBody();

// Get headers
response.getHeaders().getFirst("X-Custom-Header");
```

---

## Exception Handling

```java
try {
    var response = client.someMethod(headers);
    if (!response.isSuccessful()) {
        // The gateway answered with an error status (4xx, 5xx). No exception.
    }
} catch (JbhGatewayException e) {
    String errorCode = e.getErrorCode(); // e.g., "CONNECTION_FAILED"
    String message = e.getMessage();
}
```

An HTTP error status is **not** an exception. You get a `JbhHttpResponse` with that status.
A `JbhGatewayException` means the call got no HTTP response, or was never sent:

| `getErrorCode()` | When |
|------------------|------|
| `null` | Missing or invalid `Authorization` header, or missing source service. Nothing was sent. |
| `SERIALIZATION_ERROR` | The request body could not be written as JSON. Nothing was sent. |
| `CONNECTION_FAILED` | The client could not connect to the gateway (`java.net.ConnectException`). |
| `TIMEOUT` | No response within `connectTimeout` or `requestTimeout`. |
| `IO_ERROR` | Any other network error. |
| `INTERRUPTED` | The thread was interrupted. The interrupt flag is set again. |
| `CIRCUIT_OPEN` | The circuit breaker is open. Nothing was sent. See "Circuit breaker". |

`getHttpStatusCode()` is `null` for all of these. The original error is `getCause()`.
The codes are mapped in one place: `BaseApiGatewayClient.toGatewayException`.

### Retries

- `GET`, `PUT`, `DELETE`: retried on `502`, `503`, `504`, `429`, a timeout, or a network error.
- `POST`, `PATCH`: retried only when the connection failed (the request was never sent).
  A `POST` can have side effects, such as an email. It is never sent twice.
- Up to 3 attempts. Waits 500 ms, then 1 s.

### Circuit breaker

Each domain client has one circuit breaker (Resilience4j). It wraps the retries, so one call with
all its retries counts once.

- **Failure:** no HTTP response (connection error, timeout), or status `502`, `503`, `504`.
  A `4xx` never counts. An interrupt never counts.
- **Opens** when at least 10 of the last 20 calls were made and 50% or more failed.
- **While open**, calls fail at once with `CIRCUIT_OPEN`. Nothing is sent.
- **After 30 s**, the next call moves it to half-open: 3 test calls decide if it closes or opens again.
- Every state change is logged at `WARN`.
- Settings: `JbhCircuitBreakerConfig` (defaults in its `Builder`).

The state lives in the client instance. **Create each domain client once and reuse it.**
A new client per request gets a new, closed breaker, so it never opens.
Each service instance has its own breaker. That is fine: it protects only the caller.

**Name:** `<sourceService>.<domain>`, for example `JBH_FINANCE_API.User` or `JBH_IAM_API.Notification`.

**Metrics:** pass a Micrometer `MeterRegistry` to the builder (`meterRegistry(...)`). The breaker then
publishes the standard Resilience4j metrics, tagged `name`. In Prometheus:

| Metric | Meaning |
|--------|---------|
| `resilience4j_circuitbreaker_state{state="closed\|open\|half_open\|..."}` | `1` for the current state |
| `resilience4j_circuitbreaker_calls_seconds_count{kind="successful\|failed\|ignored"}` | Calls by result |
| `resilience4j_circuitbreaker_not_permitted_calls_total` | Calls rejected while open |
| `resilience4j_circuitbreaker_failure_rate` | Failure rate in %, `-1` until enough calls |
| `resilience4j_circuitbreaker_buffered_calls{kind}` | Calls in the sliding window |

Without a `MeterRegistry` the breaker still works. It only publishes no metrics.

---

## Tracing

The client makes one `CLIENT` span for each call, with all retries inside it. It adds the
`traceparent` (and `tracestate`) header to the request, so the gateway joins the same trace.

- **Default: no-op.** Without `.openTelemetry(...)` there are no spans and no trace headers.
  Behavior is the same as before.
- **The caller owns the SDK and the exporter.** This library has only the OpenTelemetry API.
- **`GlobalOpenTelemetry` is not read.** Pass the instance to the builder.
- The span name is `<METHOD> <url.template>`, for example `GET /jbh-api/user-groups/{groupId}`.
  No id, query string, header or body goes into the span.
- **Status:** `5xx` and exceptions (`CONNECTION_FAILED`, `TIMEOUT`, `CIRCUIT_OPEN`, ...) set `ERROR`.
  `4xx` is not an error (`403` and `404` are normal for `findUserGroup`).
- Create the clients once and reuse them (also needed for the circuit breaker).

Quarkus (`quarkus-opentelemetry` gives the CDI bean):

```java
@ApplicationScoped
class GatewayClientConfig {
    @Produces @ApplicationScoped
    JbhUserGatewayClient userClient(io.opentelemetry.api.OpenTelemetry openTelemetry) throws JbhGatewayException {
        return JbhGatewayClientBuilder.builder()
            .baseUrl(baseUrl).sourceService("my-service")
            .openTelemetry(openTelemetry)
            .build().getUserClient();
    }
}
```

Spring Boot (the `OpenTelemetry` bean comes from the OpenTelemetry starter or the Micrometer bridge):

```java
@Bean
JbhUserGatewayClient userClient(io.opentelemetry.api.OpenTelemetry openTelemetry) throws JbhGatewayException {
    return JbhGatewayClientBuilder.builder()
        .baseUrl(baseUrl).sourceService("my-service")
        .openTelemetry(openTelemetry)
        .build().getUserClient();
}
```

---

## Configuration

```java
var builder = JbhGatewayClientBuilder.builder()
    .baseUrl("https://gateway.jbh.com")      // Required
    .sourceService("order-service")           // Sets JBH-XXX-REQ-SOURCE header
    .connectTimeout(Duration.ofSeconds(5))    // Default: 10s
    .requestTimeout(Duration.ofSeconds(15))   // Default: 30s
    .meterRegistry(meterRegistry)             // Optional: circuit breaker metrics
    .openTelemetry(openTelemetry)             // Optional: tracing. Default: no-op
    .build();
```

---

## Testing Guide

### Test Structure

Tests are organized in two packages:

| Package | Purpose |
|---------|---------|
| `test/.../client/<domain>/` | **Client tests** - Simulate library callers, validate public API |
| `test/.../internal/` | **Internal tests** - Cross-client infrastructure, integration tests |

### Step 5: Create Client Tests

Location: `src/test/java/com/jbh/gateway/client/<domain>/`

Every new client **MUST** have a corresponding test class that validates:

1. **Header validation** - Invalid/missing Authorization header throws `JbhGatewayException`
2. **Request DTOs** - Builder pattern works correctly, all fields are populated
3. **Enums/Types** - All expected values exist
4. **Integration** (optional) - Actual API calls work with valid tokens

```java
public class JbhXxxGatewayClientTest {

    static JbhXxxGatewayClient client;

    static final String GOOD_TOKEN = "Bearer eyJhbGciOiJIUzUxMiJ9...";

    @BeforeAll
    public static void setup() {
        JbhGatewayClientBuilder clientBuilder =
            JbhGatewayClientBuilder.builder()
                .baseUrl("http://localhost:8080")
                .sourceService("test-service")
                .build();
        try {
            client = clientBuilder.getXxxClient();
        } catch (JbhGatewayException e) {
            throw new RuntimeException(e);
        }
    }

    // REQUIRED: Test invalid Authorization header
    @Test
    public void shouldThrowExceptionWhenNotValidAuthorizationHeader() {
        assertThrows(
            JbhGatewayException.class,
            () -> client.someMethod(Map.of("Authorization", "invalid-no-bearer"))
        );
    }

    // REQUIRED: Test missing Authorization header
    @Test
    public void shouldThrowExceptionWhenMissingAuthorizationHeader() {
        assertThrows(
            JbhGatewayException.class,
            () -> client.someMethod(Map.of())
        );
    }

    // REQUIRED: Test request DTO builder (if applicable)
    @Test
    public void shouldBuildRequestWithAllFields() {
        MyRequest request = MyRequest.builder()
            .field1("value1")
            .field2(UUID.randomUUID())
            .build();

        assertEquals("value1", request.field1());
        assertNotNull(request.field2());
    }

    // OPTIONAL: Integration test (requires running server)
    // Remove @Test annotation for CI/CD - run manually
    public void shouldCallApiSuccessfully() {
        try {
            JbhHttpResponse response = client.someMethod(Map.of("Authorization", GOOD_TOKEN));
            assertEquals(200, response.getStatusCode());
        } catch (JbhGatewayException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific client tests
mvn test -Dtest=JbhUserGatewayClientTest
mvn test -Dtest=JbhNotificationGatewayClientTest

# Run all client tests
mvn test -Dtest="com.jbh.gateway.client.**"

# Run internal tests
mvn test -Dtest="com.jbh.gateway.internal.**"

# Integration tests call httpbin.org. mvn test skips them. Run them:
mvn test -Dgroups=integration -DexcludedGroups=
```

Retry and error tests use `MockWebServer` (a local server, no network).
To swap the transport in a test, pass a `JbhHttpClientAdapter` lambda to a domain client:
`new JbhUserGatewayClientImpl(config, request -> { throw new ConnectException(); })`.

### Test Checklist for New Clients

- [ ] Create test class in `test/.../client/<domain>/`
- [ ] Test invalid Authorization header (no "Bearer " prefix)
- [ ] Test missing Authorization header
- [ ] Test request DTO builder (if applicable)
- [ ] Test all enum values exist (if applicable)
- [ ] Add integration test (without @Test) for manual verification
