## ADDED Requirements

### Requirement: One CLIENT span per gateway call
The client SHALL create exactly one span of kind `CLIENT` for each call to a domain client method that sends a request. The span SHALL cover all retries of that call. Its parent SHALL be the span in `Context.current()` of the calling thread.

#### Scenario: Call with retries
- **WHEN** a `GET` is retried twice and then returns `200`
- **THEN** one span is recorded, not three

#### Scenario: Circuit open
- **WHEN** the circuit breaker is open and a call fails with `CIRCUIT_OPEN`
- **THEN** one span is recorded, with status `ERROR` and `error.type` set, and nothing was sent

### Requirement: Span name and attributes
The span name SHALL be `<METHOD> <url.template>`. The attributes SHALL be `http.request.method`, `url.template`, `server.address` (host of the request URI), and `http.response.status_code` once a reply arrives. When the request has no template, the span name SHALL be `<METHOD>` only and `url.template` SHALL NOT be set. The client SHALL NOT put a header value, token, query string, body, raw path, or user data in any attribute or event.

#### Scenario: Find user id
- **WHEN** `findUserId` is called and the gateway answers `200`
- **THEN** the span name is `GET /jbh-api/users/find-user-id`, `url.template` is `/jbh-api/users/find-user-id`, and `http.response.status_code` is `200`

#### Scenario: User group with an id
- **WHEN** `findUserGroup` is called for group `a1b2` and the gateway answers `403`
- **THEN** the span name is `GET /jbh-api/user-groups/{groupId}`, and no attribute holds `a1b2`

#### Scenario: Notification with a query string
- **WHEN** `sendNotification` is called with type `EMAIL`
- **THEN** the span name is `POST /jbh-api/notifications/v1`, and no attribute holds `type=EMAIL`

#### Scenario: Request without template
- **WHEN** a request has no `uriTemplate`
- **THEN** the span name is `GET`, and no `url.template` attribute exists

### Requirement: Span status
The span status SHALL be `ERROR` when the call throws an exception or the reply status is 5xx. `error.type` SHALL be the status code (for example `503`) or the exception class name. A 2xx, 3xx or 4xx reply SHALL leave the status unset. The client SHALL NOT call `recordException`. The span SHALL always be ended.

#### Scenario: 503 reply
- **WHEN** the gateway answers `503` after all retries
- **THEN** the status is `ERROR`, `error.type` is `503`, and `http.response.status_code` is `503`

#### Scenario: Connection failure
- **WHEN** the call throws `java.net.ConnectException`
- **THEN** the status is `ERROR`, `error.type` is `java.net.ConnectException`, and the exception is thrown again unchanged

#### Scenario: 404 reply
- **WHEN** the gateway answers `404`
- **THEN** the status is unset and `http.response.status_code` is `404`

### Requirement: Context injection
The client SHALL inject the trace context of the new span into the request headers, with the propagators of the configured `OpenTelemetry` instance. The injected headers SHALL replace any header with the same name (case-insensitive), so a request never carries two `traceparent` headers. The request headers of the caller SHALL otherwise stay unchanged.

#### Scenario: Header sent
- **WHEN** an SDK-backed instance with the W3C propagator is configured and a call is made
- **THEN** the request sent to the gateway has one `traceparent` header whose trace id is the span's trace id

#### Scenario: Caller already sent a traceparent
- **WHEN** the caller headers hold `Traceparent: 00-aaaa...-bbbb...-01`
- **THEN** the sent request has exactly one `traceparent`, the one of the new span

### Requirement: No-op default
`JbhGatewayClientBuilder` SHALL default to `OpenTelemetry.noop()`. `openTelemetry(null)` SHALL also give the no-op instance. The client SHALL NOT read `GlobalOpenTelemetry`. With the no-op instance, the requests, the responses, the exceptions and the retries SHALL be the same as before this change, and no trace header SHALL be added.

#### Scenario: Builder without OpenTelemetry
- **WHEN** a client is built without `openTelemetry(...)`
- **THEN** `getOpenTelemetry()` is the no-op instance and a request carries no `traceparent` header

#### Scenario: Existing suite
- **WHEN** the existing test suite runs with `mvn test`
- **THEN** all tests pass unchanged
