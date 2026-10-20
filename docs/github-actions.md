# GitHub Actions audit (2026-10-20)

CI builds and tests on pull requests and pushes to `main`. Both CI and Release
produce application JARs, distribution archives, and the Gradle-generated Docker
build context. The registry jobs download only the named `build` artifact into
`build/`, preserving `build/docker` even when Docker build-record artifacts exist.
Missing build artifacts fail the upload.

CI and Release build container images for `linux/amd64` and `linux/arm64`.
Pull requests use the cache-only exporter and skip registry logins/publication.
Changes to `release.yml` also run Release on pull requests, with a temporary
`v0.0.0-pr-<number>` tag. Pushes to `release/v*` retain the existing publication
behavior: the branch suffix (including `v` and any prerelease suffix) becomes the
GHCR and Docker Hub tag. CI pushes to `main` still publish `latest`.
Dependency submission runs only on pushes to `main`, where its write token is
available, rather than attempting submission from fork/Dependabot pull requests.

## Version decisions

All actions are pinned to the release commit, with the release version beside
it. GitHub-hosted `ubuntu-latest` runners supply the Node 24-capable runner needed
by these actions; this configuration does not target older self-hosted runners.

| Component | Previous | Selected | Decision |
| --- | --- | --- | --- |
| checkout | v4 | v7.0.1 | Current stable release; existing push/pull_request events remain supported. |
| setup-java | v4 | v6.1.0 | Current stable release; retains Temurin 17. |
| upload-artifact | v4 | v7.0.2 | Retains archived directory uploads. |
| download-artifact | v4 | v8.0.2 | Explicit artifact name/path; digest mismatches fail by default. |
| Gradle setup/dependency submission | v3.4.2 | v5.0.2 | Last MIT release before v6 introduced separate proprietary cache terms. Reassess v6 separately. |
| Docker QEMU | v3 | v4.4.0 | Current stable release. |
| Docker Buildx | v3 | v4.4.1 | Current stable release; retains the explicit two-platform builder. |
| Docker login | v3 | v4.6.0 | Current stable release; retains existing credentials. |
| Docker build/push | v6 | v7.4.0 | Current stable release; retains path context and cache-only/registry exporters. |
| JDK / container runtime | 17 | 17 | Keep the LTS target shared by `jvmToolchain(17)` and distroless Java 17. Updating an Action runtime does not require changing the application's Java target. |
| Gradle Wrapper | 8.5 | 8.5 | Retain the application's existing wrapper for this workflow update. Build, tests and Docker-context generation are checked with this version; a Gradle major upgrade needs a separate Kotlin/Docker-plugin compatibility review. |

## Validation

Run `actionlint`, then `./gradlew --no-daemon build dockerCreateDockerfile`.
Confirm both workflow build jobs and both multi-platform registry jobs succeed
on the PR. Release publishing and dependency submission require their trusted
push events and cannot be established by a PR's cache-only builds.

## Upstream references

- [GitHub checkout releases](https://github.com/actions/checkout/releases)
- [Java setup releases](https://github.com/actions/setup-java/releases)
- [Artifact upload releases](https://github.com/actions/upload-artifact/releases)
- [Artifact download releases](https://github.com/actions/download-artifact/releases)
- [Gradle Actions v5.0.2](https://github.com/gradle/actions/releases/tag/v5.0.2)
- [Gradle Actions v6 license change](https://github.com/gradle/actions/releases/tag/v6.0.0)
- [Gradle Java compatibility](https://docs.gradle.org/8.5/userguide/compatibility.html)
- [Docker QEMU releases](https://github.com/docker/setup-qemu-action/releases)
- [Docker Buildx releases](https://github.com/docker/setup-buildx-action/releases)
- [Docker login releases](https://github.com/docker/login-action/releases)
- [Docker build/push releases](https://github.com/docker/build-push-action/releases)
