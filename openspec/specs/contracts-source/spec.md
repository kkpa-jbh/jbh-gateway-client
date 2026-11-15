# contracts-source Specification

## Purpose
TBD - created by archiving change jbh-gateway-client-move-contracts-source. Update Purpose after archive.
## Requirements
### Requirement: Contracts come from the jbh-notification package repo
The `github` Maven profile SHALL read `jbh-notification-contracts` from `https://maven.pkg.github.com/kkpa-jbh/jbh-notification`. The dependency coordinates SHALL stay `com.jbh:jbh-notification-contracts:1.0.0-SNAPSHOT`.

#### Scenario: Profile repository URL
- **WHEN** `pom.xml` is read
- **THEN** the repository `github-packages-read` in profile `github` has the URL `https://maven.pkg.github.com/kkpa-jbh/jbh-notification`

#### Scenario: No old source left
- **WHEN** the repo is searched for `jbh-personal-finance` as the contracts source
- **THEN** no hit remains (pom.xml, workflow, README)

### Requirement: Docs name the new source
The `publish.yml` header and the README "Install and publish" section SHALL say the contracts are published by `jbh-notification`.

#### Scenario: Workflow header
- **WHEN** `.github/workflows/publish.yml` is read
- **THEN** the header comment names `jbh-notification` (workflow `publish`) as the contracts publisher

