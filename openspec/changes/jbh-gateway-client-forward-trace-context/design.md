## Context

JBH-55 section 2 (sub-issue JBH-57). The client sends no trace context today. Chain today: `JbhGatewayInternalClientImpl` → `CircuitBreakerHttpClientAdapter` → `NativeHttpClientAdapter` (java.net.http, with retries). Callers are Quarkus (`jbh-personal-finance`) and Spring Boot (`jbh-iam`). Only the `OpenTelemetry` API is added. The caller owns the SDK and the exporter.

## Goals / Non-Goals

**Goals:** one CLIENT span per call; `traceparent` / `tracestate` on the request; no change in behavior with the no-op instance; a small, documented public API change.

**Non-Goals:** see "Not included".

## Decisions

- **D1 Span placement: outermost.** New `TracingHttpClientAdapter` in `internal/core/http/adapter/`. Chain: Tracing → CircuitBreaker → Native. One span per logical call. Retries and breaker are inside it. A `CIRCUIT_OPEN` call (nothing sent) still ends as an error span. Rejected: span inside the breaker (a rejected call would leave no span) and a span per attempt (too many spans, noisy).
- **D2 `url.template` is explicit.** Each domain client declares its path template. It travels down in `JbhHttpRequest` as an optional internal field `uriTemplate`. The adapter never guesses from the raw path. The query string is never part of a template. Templates (with the `/jbh-api` prefix, added by `BaseApiGatewayClient`): `/jbh-api/users/find-user-id`, `/jbh-api/user-groups/{groupId}`, `/jbh-api/notifications/v1`. Fallback when a request has no template: span name is `<METHOD>` only and `url.template` is not set. The raw path is never used. Rejected: guessing from the path (a group id would leak into a name and make high-cardinality names).
- **D3 SOLID.** SRP: tracing lives only in the new decorator. OCP: `NativeHttpClientAdapter` does not change. DIP: the decorator depends on the `io.opentelemetry.api.OpenTelemetry` interface, injected through the builder. The decorator implements the existing `JbhHttpClientAdapter`; no new interface is added.
- **D4 Dependency.** `io.opentelemetry:opentelemetry-api` only. The BOM `io.opentelemetry:opentelemetry-bom` is imported in `dependencyManagement` (scope `import`). Version in property `opentelemetry.version` = `1.62.0`. Like Micrometer, the caller's BOM may pick another version at runtime. The API is backward compatible.
- **D5 JPMS.** The jars are automatic modules `io.opentelemetry.api` and `io.opentelemetry.context`. `OpenTelemetry` is in the public builder API, so `requires transitive io.opentelemetry.api;`. `Context` and `TextMapSetter` are in `io.opentelemetry.context`: `requires io.opentelemetry.context;` (no `transitive`: no public signature uses them).
- **D6 No global.** `GlobalOpenTelemetry` is never read. Spring Boot does not register its instance there. The caller passes the instance. Default: `OpenTelemetry.noop()`. `openTelemetry(null)` gives noop too, so `getOpenTelemetry()` never returns `null`.
- **D7 Span details.** `tracer.spanBuilder(name).setSpanKind(SpanKind.CLIENT)`. Parent: `Context.current()` (the caller's server span). The tracer is created once in the constructor: `openTelemetry.getTracer("com.jbh.gateway")`. No version (no cheap source for it). Attributes: `http.request.method`, `url.template` (only when known), `server.address` (host of the URI), `http.response.status_code` after the reply. Flow: start the span, `makeCurrent()` in try-with-resources for the `Scope`, `end()` in `finally` (a `Span` is not `AutoCloseable`).
- **D8 Errors.** Status `ERROR` on an exception or a 5xx reply. `error.type` = the status code (`"503"`) or the exception class name (`getClass().getName()`). 4xx leaves the status unset: the client got a valid answer, and `403` / `404` are normal results for `findUserGroup`. `recordException` is **not** called: an exception message can hold a URL or user data. The exception is thrown again unchanged. Never put a header value, token, query string, body or user data in an attribute or event.
- **D9 Injection.** `openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current().with(span), carrier, setter)`. The carrier is a `Map<String,String>`. The setter puts the value. Then the decorator builds a new request with the carrier entries **replacing** headers of the same name. `JbhHttpHeaders` has only `with` (adds), so a new method `JbhHttpHeaders.withReplaced(name, value)` is added: `builder().addAll(this).set(name, value).build()`. Its builder is a case-insensitive `TreeMap`, so `Traceparent` from the caller is replaced too. `JbhHttpRequest` gets a copy method `withHeaders(JbhHttpHeaders)` that keeps all other fields, including `uriTemplate`. With the no-op instance the carrier stays empty, so the request is passed on as is (same object).
- **D10 Template plumbing.** `JbhHttpRequest.Builder.uriTemplate(String)` and `getUriTemplate()` (nullable). `equals`, `hashCode`, `toString` include it. `JbhGatewayInternalClientImpl` gets `get/post/put/delete` overloads with a `uriTemplate` argument. The old signatures stay and call the new ones with `null`. `BaseApiGatewayClient` gets `executeGet/Post/Put/Delete` overloads `(path, uriTemplate, ...)`. It adds the `/jbh-api` prefix to the template, like it does for the path. The old overloads stay and pass `null` (fallback of D2). The domain clients move to the new overloads.
- **D11 Test constructors.** `...Impl(builder, adapter)` keeps working. The injected test adapter is the transport. Tracing wraps outside the breaker, and it is a no-op unless the test builder passes an `OpenTelemetry`.
- **D12 Version.** The repo has no version rule: everything is `1.0.0-SNAPSHOT` and CI pulls SNAPSHOTs with `-U`. The change is additive and backward compatible, so the version stays `1.0.0-SNAPSHOT`. The `publish` workflow runs after merge.
- **D13 System map.** `../jbh-deploy/README.md` is not edited: no new method, no changed path, no contracts change (no "keep in sync" trigger).

## Responsibilities

| File | Responsibility | Changes when |
|------|----------------|--------------|
| `pom.xml` | Dependencies | a library version changes |
| `module-info.java` | Module requires | module dependencies change |
| `JbhGatewayClientBuilder.java` | Caller config. New optional `openTelemetry(OpenTelemetry)`, default `OpenTelemetry.noop()`, plus getter | caller options change |
| NEW `TracingHttpClientAdapter.java` | One CLIENT span per call and injection of the context into the request headers | tracing rules change |
| `JbhHttpRequest.java` | Immutable request. New optional `uriTemplate` and `withHeaders` | request shape changes |
| `JbhHttpHeaders.java` | Immutable header map. New `withReplaced` | header operations change |
| `JbhGatewayInternalClientImpl.java` | Builds the request, passes the template | request building changes |
| `BaseApiGatewayClient.java` | Wires the adapter chain, passes the template | the chain changes |
| `JbhUserGatewayClientImpl.java`, `JbhNotificationGatewayClientImpl.java` | Declare their path templates | an endpoint changes |
| `README.md` (new "Tracing" section), `CLAUDE.md` (Dependencies list, Key Patterns line) | Docs | docs change |

`TracingHttpClientAdapter` stays small by splitting into helpers: `execute` (flow), `startSpan`, `injectContext`, `recordReply`, `recordFailure`. Each is a few lines with at most one branch.

## Concurrency

none. `TracingHttpClientAdapter` holds only final fields (`delegate`, `OpenTelemetry`, `Tracer`). Each call keeps its span, scope and carrier in local variables. The `Tracer` and the propagator are thread-safe by the OpenTelemetry API contract. The `Scope` is opened and closed on the calling thread, so no context leaks to another thread. No shared mutable state, no async code, no check-then-act.

## Database

No database change.

## Test contract

No new tests (user decision: unit tests excluded for JBH-55). The existing suite (`mvn test`) must pass unchanged. That is the expected result. No test file is changed or deleted.

Expected values for a manual check by the user (not automated), with an SDK-backed `OpenTelemetry` that uses the W3C propagator and an in-memory exporter:

| Input | Expected |
|-------|----------|
| `findUserId`, gateway `200` | span `GET /jbh-api/users/find-user-id`, kind `CLIENT`, `url.template=/jbh-api/users/find-user-id`, `http.response.status_code=200`, status unset, request has one `traceparent` |
| `findUserGroup("a1b2")`, `403` | span `GET /jbh-api/user-groups/{groupId}`, status code `403`, status unset, no attribute contains `a1b2` |
| `sendNotification` type `EMAIL`, `503` ×3 | one span `POST /jbh-api/notifications/v1`, status `ERROR`, `error.type=503`, no attribute contains `type=EMAIL` |
| Connection refused | one span, status `ERROR`, `error.type=java.net.ConnectException`, `JbhGatewayException` code `CONNECTION_FAILED` as before |
| Breaker open | one span, status `ERROR`, nothing sent, `JbhGatewayException` code `CIRCUIT_OPEN` as before |
| Caller header `Traceparent: x` | one `traceparent` header on the wire, the one of the new span |
| No `openTelemetry(...)` call | no `traceparent` header, same behavior as before |

Static checks: `grep -rn "GlobalOpenTelemetry" src/main` has 0 hits; `grep -rn "recordException" src/main` has 0 hits.

## Risks / Trade-offs

- The span covers the retry waits (500 ms, 1 s), so its duration includes them. This is wanted: it is the time the caller waits.
- A caller whose BOM has an older OpenTelemetry API than `1.62.0` may fail at runtime if it lacks a method. Only long-stable API methods are used (`getTracer`, `spanBuilder`, `getPropagators`, `Context`).
- Old overloads without a template give a name without the path. Domain clients must use the new overloads. The README "Adding a New Domain Client" part names this rule.

## Not included

- OpenTelemetry SDK and exporter (the caller owns them).
- Metrics from the client.
- A split of `BaseApiGatewayClient` (it has older jobs: config, chain, error mapping). Not done in this change.
- Wiring in the consumers: `jbh-iam` and `jbh-personal-finance` (JBH-55 sections 4 and 5).
- A version bump or a new version rule.
