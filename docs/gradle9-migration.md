# Gradle 9 and Docker plugin migration

## Selected versions

| Component | Previous | Selected |
| --- | --- | --- |
| Gradle Wrapper | 8.14.6 | 9.2.1 |
| Docker Java Application plugin | 9.4.0 | 10.0.0 |
| Kotlin JVM plugin and BOM | 2.2.20 | 2.3.21 |
| Build JVM, application target and container runtime | 17 | 17 |

Docker plugin 10.0.0 requires Gradle 9.2 or later. Gradle 9.2.1 is the
patch release of that minimum supported line. Kotlin 2.2.20 only fully supports
Gradle through 8.14; Kotlin 2.3.21 supports 7.6.3 through 9.3.0. Keep the Kotlin
BOM aligned with the compiler plugin. Gradle 9 requires Java 17 or later to run;
the existing JDK 17 CI jobs and `jvmToolchain(17)` meet that requirement.

Docker plugin 10 updates Docker Java from 3.3.3 to 3.6.0 and removes deprecated
`DockerExecContainer` methods. This project does not use that task. These are
build-plugin dependencies and do not become application runtime dependencies.
The wrapper distribution is pinned with the upstream SHA-256 checksum, and the
wrapper scripts and JAR are regenerated using the selected Gradle version.

## Validation

Local validation on JDK 17 passed: `build` (15 tests, no failures or skips) and
`dockerCreateDockerfile`, with no deprecation warnings under `--warning-mode all`.
The runtime dependency report changes only Kotlin stdlib and reflect from
2.2.20 to 2.3.21; other application dependency versions remain unchanged.
The generated Dockerfile retains the base image, port and entrypoint below.

Run with JDK 17 and the repository-local Gradle cache:

```sh
GRADLE_USER_HOME="$PWD/.gradle-user-home" ./gradlew --no-daemon build dockerCreateDockerfile --warning-mode all
```

Inspect `build/docker/Dockerfile` for the distroless Java 17 Debian 12 base,
port 2112, and the `dev.westelh.obe.Main` entrypoint. Compare runtime dependency
reports before and after the update. CI's registry job builds the generated
context for both `linux/amd64` and `linux/arm64`, using cache-only output on PRs.

## References

- [Docker plugin 10.0.0 changes](https://github.com/bmuschko/gradle-docker-plugin/blob/v10.0.0/src/docs/asciidoc/user-guide/40-changes.adoc)
- [Kotlin Gradle compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Gradle 9.2.1 compatibility](https://docs.gradle.org/9.2.1/userguide/compatibility.html)
- [Gradle distribution checksums](https://gradle.org/release-checksums/)
