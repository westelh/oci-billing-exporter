# Project Instructions

## Additional Agent Instructions

- Before starting work, read and follow [Linear・GitHub連携ルール](agent-instructions/linear-github.md).

## Pull Request and Commit Language

- Write pull request titles and descriptions in English.
- Write commit message subjects and bodies in English.

## Gradle Cache

- When running Gradle, set `GRADLE_USER_HOME` to `.gradle-user-home` at the repository root. For example, run `GRADLE_USER_HOME="$PWD/.gradle-user-home" ./gradlew build` from the repository root.
- Keep the cache after execution and reuse it for subsequent runs.
- Exclude `.gradle-user-home/` from Git version control.
- Create and use the cache without asking for additional confirmation when permitted by the sandbox. For operations requiring separate permission, follow the actual permission restrictions.

