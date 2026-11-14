## Why

The `jbh-notification-contracts` library moves out of `jbh-personal-finance` into the new repo `jbh-notification` (JBH-74). The coordinates stay `com.jbh:jbh-notification-contracts:1.0.0-SNAPSHOT`. Only the GitHub Packages repo that publishes it changes. This repo must read it from the new place.

## What Changes

- `pom.xml`: profile `github` reads the package repo `https://maven.pkg.github.com/kkpa-jbh/jbh-notification`. The comment above the profile is updated.
- `.github/workflows/publish.yml`: the header comment says the contracts come from `jbh-notification` (workflow `publish`).
- `README.md`: "Install and publish" names `jbh-notification` as the contracts source.
- No Java code change. No dependency coordinate change.

## Capabilities

### New Capabilities
- `contracts-source`: where the build finds `jbh-notification-contracts` in CI (`-Pgithub`).

### Modified Capabilities
None.

## Impact

- CI `publish` needs the contracts published first from `jbh-notification`.
- Token `PACKAGES_READ_TOKEN` must be able to read `jbh-notification` packages (user step).
- Local `mvn install` is not affected, except the contracts must be installed from `jbh-notification`.
