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

src/test/java/com/jbh/gateway/
├── client/                          # CLIENT TESTS (simulates library callers)
│   ├── users/
│   │   └── JbhUserGatewayClientTest.java
│   └── notifications/
│       └── JbhNotificationGatewayClientTest.java
│
└── internal/                        # INTERNAL TESTS (cross-client, infrastructure)
    ├── http/
    │   ├── HttpClientFactoryTest.java
    │   └── MockHttpClientAdapter.java
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

    public JbhXxxGatewayClientImpl(JbhGatewayClientBuilder config)
        throws JbhGatewayException {
        super(config);
    }

    @Override
    public JbhHttpResponse myMethod(Map<String, String> clientHttpHeaders)
        throws JbhGatewayException {
        JbhHttpHeaders headers = JbhHttpHeaders.fromMap(clientHttpHeaders);
        validator.validateAuthorizationHeader(headers);
        return executeGet(MY_PATH, headers);
    }

    @Override
    public JbhHttpResponse createSomething(MyRequest request, Map<String, String> clientHttpHeaders)
        throws JbhGatewayException {
        JbhHttpHeaders headers = JbhHttpHeaders.fromMap(clientHttpHeaders);
        validator.validateAuthorizationHeader(headers);
        String jsonBody = JacksonJsonUtil.toJson(request);
        return executePost(MY_PATH, jsonBody, headers);
    }
}
```

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
  Install `jbh-notification-contracts` first (from `jbh-personal-finance`).
- **CI:** `.github/workflows/publish.yml` runs `mvn -Pgithub deploy` on every push to `main`.
  It publishes to GitHub Packages (`https://maven.pkg.github.com/kkpa-jbh/jbh-gateway-client`).
  The `github` profile reads `jbh-notification-contracts` from the `jbh-personal-finance` package.
- **CI order:** contracts (`jbh-personal-finance` → `publish-contracts`) → this library → `jbh-iam` and `jbh-personal-finance` images.

## BaseApiGatewayClient Methods

The base class provides these protected methods:

| Method | Description |
|--------|-------------|
| `executeGet(path, headers)` | GET request to `/jbh-api{path}` |
| `executePost(path, body, headers)` | POST request with JSON body |
| `executePut(path, body, headers)` | PUT request with JSON body |
| `executeDelete(path, headers)` | DELETE request |

All paths are automatically prefixed with `/jbh-api`.

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
} catch (JbhGatewayException e) {
    String errorCode = e.getErrorCode();        // e.g., "CONNECTION_FAILED"
    Integer httpStatus = e.getHttpStatusCode(); // e.g., 503
    String message = e.getMessage();
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

# Run internal/integration tests
mvn test -Dtest="com.jbh.gateway.internal.**"
```

### Test Checklist for New Clients

- [ ] Create test class in `test/.../client/<domain>/`
- [ ] Test invalid Authorization header (no "Bearer " prefix)
- [ ] Test missing Authorization header
- [ ] Test request DTO builder (if applicable)
- [ ] Test all enum values exist (if applicable)
- [ ] Add integration test (without @Test) for manual verification
