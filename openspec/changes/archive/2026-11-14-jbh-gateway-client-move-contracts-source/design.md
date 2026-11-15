## Context

JBH-74 section 3. The contracts move to `kkpa-jbh/jbh-notification`. This is a text change in three files. No database, no Java code, no new class.

## Goals / Non-Goals

**Goals:** nothing in this repo points to `jbh-personal-finance` as the contracts source.
**Non-Goals:** Java code, dependency coordinates, `JbhNotificationGatewayClientImpl`, circuit breaker `JBH_IAM_API.Notification`.

## Decisions

- D1: keep the coordinates `com.jbh:jbh-notification-contracts:1.0.0-SNAPSHOT`. Only the read repository URL in profile `github` changes, because only the publishing repo moves.
- D2: only contracts-source mentions change. Consumer or caller mentions of `jbh-personal-finance` (README 31, 32, 166, images part of 171) stay, because they are not about the contracts source (table below).
- D3: no unit or integration tests, because no Java code changes. Verification is the grep counts plus the user's `mvn install` and CI `publish` (see Test contract).

Search of the whole repo (`grep -rn jbh-personal-finance`, excluding `.git`, `target`) found these hits. `CLAUDE.md` and `.github/` have no other hit than the one below.

| File:line | Hit | Contracts source? | Action |
|-----------|-----|-------------------|--------|
| `pom.xml:184` | comment "published by the jbh-personal-finance repo" | yes | change to `jbh-notification` |
| `pom.xml:191` | repository URL `.../jbh-personal-finance` | yes | change to `.../jbh-notification` |
| `.github/workflows/publish.yml:2` | header comment | yes | update (contracts from `jbh-notification`, workflow `publish`) |
| `README.md:167` | "Install `jbh-notification-contracts` first (from `jbh-personal-finance`)" | yes | change to `jbh-notification` |
| `README.md:170` | "reads ... from the `jbh-personal-finance` package" | yes | change to `jbh-notification` |
| `README.md:171` | CI order "contracts (`jbh-personal-finance` -> `publish-contracts`)" | yes | change to `jbh-notification` -> `publish` |
| `README.md:31`, `README.md:32` | "Used by `jbh-personal-finance`" (client methods table) | no, it is a caller of the user client | leave alone |
| `README.md:166` | "`jbh-iam` and `jbh-personal-finance` find it there" (~/.m2) | no, they are consumers of this library | leave alone |
| `README.md:171` | "... `jbh-iam` and `jbh-personal-finance` images" (end of line) | no, consumers | leave alone |
| `.github/workflows/publish.yml:3` | "`jbh-iam` and `jbh-personal-finance` then download `com.jbh:jbh-gateway-client`" (added by commit `b450ef0`, after this table) | no, consumers | leave alone (user decision D7: the one allowed `.github/` hit) |

## Test contract

No Java code changes, so there are no unit tests and no integration tests (user decision in the system plan: unit tests excluded). The test blueprint group does not apply: Test-First is on, but there is nothing to test.

Verification instead:
- `grep -rn "maven.pkg.github.com/kkpa-jbh/jbh-personal-finance" . --exclude-dir=.git --exclude-dir=target` returns **0** hits.
- `grep -rn "maven.pkg.github.com/kkpa-jbh/jbh-notification" pom.xml` returns **1** hit.
- `grep -rn "jbh-personal-finance" pom.xml .github` returns **0** hits.
- User runs `mvn install` locally (contracts installed from `jbh-notification` first).
- User checks the CI `publish` workflow is green, after section 2 published the contracts from `jbh-notification`.

## Database design

none. No database object changes.

## Responsibilities

none. No new class.

## Concurrency

none. No code, no shared data.

## Risks / User verification

- User step, not a task: `PACKAGES_READ_TOKEN` (classic PAT with `read:packages`, org-wide) must read `jbh-notification` packages. If CI cannot resolve the contracts, check this first.
- Order: CI `publish` is green only after section 2 published the contracts from `jbh-notification`.

## Not included

- `../jbh-deploy/README.md` and `CI.md`: the system map is updated in section 7 of the system plan.
