# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JBH Gateway Client is a Java 21 HTTP client library that provides type-safe, domain-specific clients for JBH microservices. It uses Java Platform Module System (JPMS) to enforce encapsulation—only `com.jbh.gateway.client.*` packages are exported; all implementation details in `internal/` are hidden.

## Build Commands

```bash
# Build and run all tests
mvn test

# Run specific client test
mvn test -Dtest=JbhUserGatewayClientTest

# Run all client tests
mvn test -Dtest="com.jbh.gateway.client.**"

# Run integration tests only (requires network)
mvn test -Dgroups=integration

# Exclude integration tests
mvn test -DexcludedGroups=integration
```

## Architecture

### Public API vs Internal Implementation

```
com.jbh.gateway.client/          # PUBLIC - exported via module-info.java
├── JbhGatewayClientBuilder      # Entry point, creates all domain clients
├── JbhGatewayException          # Checked exception with error code + HTTP status
├── JbhHttpResponse              # Response wrapper with typed body access
├── users/                       # User domain interface
└── notifications/               # Notification domain interface

com.jbh.gateway.internal/        # PRIVATE - not exported, implementation only
├── domains/
│   ├── BaseApiGatewayClient     # Base class all clients extend
│   └── {domain}/Impl classes
└── core/                        # HTTP infrastructure (adapter, headers, validators)
```

### Key Patterns

- **All paths auto-prefixed with `/jbh-api`** in `BaseApiGatewayClient`
- **Headers accepted as `Map<String, String>`** in public API, converted to `JbhHttpHeaders` internally
- **Adapter pattern** for HTTP clients (currently `NativeHttpClientAdapter` using java.net.http)
- **Configuration Injection** via `JbhGatewayClientBuilder`

## Adding a New Domain Client

1. Create interface in `client/{domain}/Jbh{Domain}GatewayClient.java`
2. Create implementation in `internal/domains/{domain}/Jbh{Domain}GatewayClientImpl.java` extending `BaseApiGatewayClient`
3. Add factory method in `JbhGatewayClientBuilder`: `get{Domain}Client()`
4. Export package in `module-info.java`
5. Create tests in `test/client/{domain}/`

### Method Signature Standards

All client methods must:
- Return `JbhHttpResponse`
- Throw `JbhGatewayException` (checked)
- Accept `Map<String, String>` for headers
- Call `validator.validateAuthorizationHeader(headers)` before execution

## Dependencies

- **jbh-notification-contracts**: External contract dependency (required transitive module)
- **Publishing**: local `mvn install`; CI publishes to GitHub Packages with `mvn -Pgithub deploy` (see `README.md` "Install and publish")
- **Jackson**: JSON serialization
- **java.net.http**: Native HTTP client (no external HTTP libs in main scope)
- **OkHttp MockWebServer**: Test dependency only

## System map sync (`../README.md`)

This repo is one part of the JBH system. The map of how all JBH services talk to each other
lives in `../README.md` (relative to this repo's root: the `kkpa-jbh` folder that holds all JBH repos).

**Rule:** when a change in this repo affects how services communicate, update `../README.md`
in the same task. Change only the rows or lines that are affected. Then tell the user that
`../README.md` changed, because that folder is not a git repo and the change is not versioned.

Triggers for this repo:
- Add a new domain client or method (this is a new service-to-service call)
- Change a path a client calls (for example `/users/find-user-id`, `/notifications/v1`)
- Change the dependency on `jbh-notification-contracts`, the groupId/artifactId, or the version (this changes the build order)

If you are not sure a change counts, read `../README.md` and check whether any line is now wrong.
