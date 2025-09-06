# Repository Guidelines

## Project Structure & Module Organization
- Modules: `app/` (Android app), `terminal-emulator/`, `terminal-view/`, `termux-shared/`. See `settings.gradle`.
- App code: `app/src/main/java/` (Java/Kotlin), `app/src/main/cpp/` (NDK), resources in `app/src/main/res/`, manifest at `app/src/main/AndroidManifest.xml`.
- Tests: `app/src/test/java/` (JUnit/Robolectric). Other modules mirror this layout.
- Build config: root `build.gradle`, `gradle.properties`, module `build.gradle` files. Release tooling in `fastlane/`. Additional docs in `docs/`.

## Build, Test, and Development Commands
- Build debug APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
- Build release APK: `./gradlew assembleRelease`.
- Install debug on device: `./gradlew :app:installDebug` (device/emulator required).
- Run unit tests: `./gradlew test` or `./gradlew testDebugUnitTest`.
- Lint & static checks: `./gradlew lint`.
- Clean workspace: `./gradlew clean`.
- Show version name (if task present): `./gradlew versionName`.

## Coding Style & Naming Conventions
- Follow `.editorconfig`: UTF-8, LF endings, 4-space indentation (YAML uses 2).
- Kotlin/Java: PascalCase for classes, camelCase for methods/fields, UPPER_SNAKE_CASE for constants.
- Android resources: `activity_main.xml`, `ic_<name>.xml/png`, string keys `filebrowser_<feature>_*`.
- Packages under `com.termux.*`; keep features grouped (e.g., `com.termux.app.filebrowser`).
- Keep functions small, handle errors via existing logging utilities.

## Testing Guidelines
- Frameworks: JUnit 4 and Robolectric for unit tests.
- Location: `app/src/test/java/...`. Name tests `*Test` and mirror package structure.
- Scope: cover core logic (view models, managers, utilities). Mock external I/O where possible.
- Run `./gradlew test` locally and ensure `./gradlew lint` passes before PRs.

## Commit & Pull Request Guidelines
- Use Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
- Keep PRs focused; include description, linked issues (e.g., `Fixes #123`), and screenshots/GIFs for UI changes.
- Update docs when behavior changes (e.g., `CLAUDE.md`, `README.md`).
- Security: never commit secrets/keys; see `SECURITY.md`. Use the provided test keystore only for debug.

## Notes for Contributors
- High-level overview and architecture: see `CLAUDE.md`.
- Task flow used in this fork: check `todos/` and `todo/` plans (e.g., `todos/task_0904_1300.md`).
