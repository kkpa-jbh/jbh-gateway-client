## 1. Test blueprint

Does not apply: no unit tests in this change (user decision). No blueprint group. The existing suite must pass unchanged.

## 2. Dependency and model (independent sub-tasks, no dependencies between them)

- [ ] 2.1 `pom.xml`: add property `opentelemetry.version` = `1.62.0`; add `dependencyManagement` that imports `io.opentelemetry:opentelemetry-bom` (type `pom`, scope `import`); add dependency `io.opentelemetry:opentelemetry-api` (no version). `module-info.java`: add `requires transitive io.opentelemetry.api;` (comment: `OpenTelemetry` is in the public builder API) and `requires io.opentelemetry.context;`. (D4, D5)
- [ ] 2.2 `JbhHttpHeaders.java`: add `withReplaced(String name, String value)` (copy, case-insensitive replace). `JbhHttpRequest.java`: add nullable field `uriTemplate`, `getUriTemplate()`, `Builder.uriTemplate(String)`, copy method `withHeaders(JbhHttpHeaders)` that keeps all fields; include it in `equals`, `hashCode`, `toString`. Existing signatures do not change. (D9, D10)
- [ ] 2.3 `JbhGatewayClientBuilder.java`: field `openTelemetry`, getter `getOpenTelemetry()`, `Builder.openTelemetry(OpenTelemetry)`. Default and `null` give `OpenTelemetry.noop()`. (D6)
- [ ] 2.4 `README.md` new section "Tracing" and `CLAUDE.md` (Dependencies: OpenTelemetry API line; Key Patterns: tracing decorator line). README: what the client adds (one CLIENT span per call, `traceparent` header), the no-op default, a Quarkus snippet (CDI-injected `io.opentelemetry.api.OpenTelemetry`) and a Spring Boot snippet (injected `OpenTelemetry` bean), the configuration block gets `.openTelemetry(...)`, the architecture tree lists the new adapter, and the rule that domain clients pass a template. (D1, D2, D6)

## 3. Tracing decorator (depends on 2.1, 2.2)

- [ ] 3.1 New `internal/core/http/adapter/TracingHttpClientAdapter.java` implementing `JbhHttpClientAdapter`. Constructor `(JbhHttpClientAdapter delegate, OpenTelemetry openTelemetry)`; creates the tracer `com.jbh.gateway` once. Implements D7, D8, D9: name and attributes, `makeCurrent()` scope, status and `error.type`, no `recordException`, header replace, `end()` in `finally`. Helpers: `startSpan`, `injectContext`, `recordReply`, `recordFailure`. Each function inside the design limits (cyclomatic 10, cognitive 15). (D1, D3, D7, D8, D9)

## 4. Wiring (depends on 2.2, 2.3, 3.1)

- [ ] 4.1 `JbhGatewayInternalClientImpl.java`: add `get/post/put/delete` overloads with `uriTemplate` (and the typed `get`). Old signatures call them with `null`. The built request gets `.uriTemplate(...)`. (D10)
- [ ] 4.2 `BaseApiGatewayClient.java`: add `executeGet/Post/Put/Delete` overloads `(path, uriTemplate, ...)`; prefix the template with `/jbh-api` like the path. Wrap the chain: `new TracingHttpClientAdapter(breakerAdapter, gatewayConfig.getOpenTelemetry())`, passed to `JbhGatewayInternalClientImpl`. Keep the test-adapter constructor working. (D1, D10, D11)
- [ ] 4.3 `JbhUserGatewayClientImpl.java`: `findUserId` uses template `/users/find-user-id`; `findUserGroup` uses `USER_GROUPS_PATH + groupId` with template `/user-groups/{groupId}`. `JbhNotificationGatewayClientImpl.java`: `sendNotification` uses the path with `?type=` and template `/notifications/v1`. (D2)

## 5. Verification (depends on 4.1, 4.2, 4.3, 2.4)

- [ ] 5.1 The user runs `mvn test`. Expected: all existing tests pass, none changed. The leader also runs `mvn -q pmd:check`. Expected: no new violation.
- [ ] 5.2 Static checks: `grep -rn "GlobalOpenTelemetry\|recordException" src/main` has 0 hits; `NativeHttpClientAdapter.java` is unchanged in `git diff`; `../jbh-deploy/README.md` is unchanged. (D3, D6, D8, D13)

Dependencies: 2.1, 2.2, 2.3, 2.4 are parallel. 3.1 needs 2.1 and 2.2. 4.1 needs 2.2. 4.2 needs 2.3 and 3.1 (and 4.1 for the new overloads). 4.3 needs 4.2. 5.x needs everything.

Size estimate: 12 files, ~330 changed lines (tests and docs included)
