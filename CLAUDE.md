# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a modified version of the Termux Android terminal application with added remote file browsing functionality. The app provides SSH/SFTP connections and includes a VSCode-style remote file browser with project workspace management, directory tree navigation, and bookmark features.

## Build Commands

### Building the App
```bash
# Build debug APK
./gradlew app:assembleDebug

# Build release APK  
./gradlew app:assembleRelease

# Clean build
./gradlew clean

# Build without Gradle daemon
./gradlew app:compileDebugJavaWithJavac --no-daemon
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run tests with Android resources
./gradlew testDebugUnitTest
```

### Code Quality
```bash
# Check for lint issues
./gradlew lint

# Generate version name
./gradlew versionName
```

## Architecture Overview

### Core Components

1. **TermuxApplication** - Main application class that initializes crash handling, logging, bootstrap setup, and shell environment
2. **MainTabActivity** - Tab-based main interface with SSH connection, file browsing, Git changes, and settings tabs
3. **RemoteFileBrowserActivity** - VSCode-style remote file browser with drawer navigation, directory tree, and file operations
4. **TermuxService** - Background service for terminal operations
5. **TermuxActivity** - Original terminal interface

### Key Packages

- `com.termux.app` - Main application activities and services
- `com.termux.app.fragments` - UI fragments for SSH connection and file browsing
- `com.termux.app.models` - Data models for SSH config, file items, workspace management
- `com.termux.app.adapters` - RecyclerView adapters for file lists and directory trees
- `com.termux.app.managers` - Business logic managers for projects and workspaces
- `com.termux.app.sftp` - SFTP connection management
- `com.termux.shared` - Shared utilities and constants across modules

### Module Structure

- **app/** - Main Android application module
- **terminal-view/** - Terminal view implementation
- **terminal-emulator/** - Terminal emulation logic  
- **termux-shared/** - Shared libraries and utilities

## Development Guidelines

### Adding New Features

When adding new functionality:

1. Follow existing package structure under `com.termux.app`
2. Use the existing `TermuxConstants` class for configuration values
3. Implement proper error handling with `Logger` class
4. Use RxJava for asynchronous operations (already included)
5. Follow Material Design guidelines for UI components

### Working with SFTP Features

The remote file browser uses:
- **SSHJ library** (com.hierynomus:sshj) for SSH/SFTP connections
- **RxJava3** for async operations
- **Gson** for JSON serialization of configs and workspaces
- **RecyclerView** with custom adapters for directory trees

Key classes:
- `SFTPConnectionManager` - Manages SFTP connections
- `ProjectWorkspaceManager` - Handles project workspaces and bookmarks
- `RemoteFileBrowserAdapter` - File list display
- `FileTreeAdapter` - VSCode-style directory tree

### Configuration Files

- **gradle.properties** - Build configuration (SDK versions, memory settings)
- **app/build.gradle** - App-specific dependencies and build settings
- **proguard-rules.pro** - Code obfuscation rules
- Bootstrap packages are downloaded dynamically based on architecture

### Key Dependencies

- AndroidX libraries for UI components
- RxJava3 for reactive programming
- SSHJ for SSH/SFTP connections
- Gson for JSON processing
- Material Design components
- Markwon for markdown rendering

### Testing

The project uses:
- JUnit 4 for unit tests
- Robolectric for Android unit tests
- Tests should be placed in `app/src/test/java/`

### Build Variants

- **Debug builds** - Use test signing key, have debugging enabled
- **Release builds** - Minified, production-ready
- **Package variants** - Support for different Android versions (apt-android-7, apt-android-5)

### Important Notes

- The app uses a shared user ID system with Termux plugins
- Bootstrap packages are architecture-specific and downloaded at build time
- SSH connections support both password and key-based authentication
- The remote file browser maintains project workspaces with persistent state
- Directory trees use lazy loading for performance with large remote directories

### Chinese Language Support

This fork includes Chinese UI text and comments. When working with the codebase:
- UI strings may be in Chinese in the source code
- Comments may be in Chinese, especially in newer remote file browser components
- Follow existing patterns for internationalization if adding new strings