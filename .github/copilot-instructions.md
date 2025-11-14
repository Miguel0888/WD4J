# WD4J - WebDriver for Java

## Project Overview

WD4J is a Java-based implementation of the WebDriver BiDi (BiDirectional) protocol, focusing on modern, native Java solutions. It provides a lightweight, fully Java-written interface as an alternative to Selenium, without the legacy overhead.

### Key Goals
- **No native executables required** - Pure Java implementation
- **Lighter than Selenium** - No backward compatibility burden
- **Playwright-compatible API** - Familiar developer experience

## Architecture

### Module Structure

The project consists of 5 main modules:

1. **wd4j** (Core Module)
   - WebDriver BiDi protocol client implementation (Java 8)
   - WebSocket-based communication with browsers
   - Session, context, input, network, and script management
   - Event-driven architecture with bidirectional messaging
   - Location: `/wd4j/`

2. **playwright-java** (API Interfaces)
   - Playwright-compatible interface definitions
   - No dependencies on implementation
   - Provides: `Playwright`, `Browser`, `BrowserContext`, `Page`, `Locator`, etc.
   - Location: `/playwright-java/`

3. **playwright-adapter** (Implementation)
   - Concrete implementations of playwright-java interfaces
   - Bridge between Playwright API and wd4j core
   - Implements: `BrowserImpl`, `PageImpl`, `LocatorImpl`, etc.
   - Location: `/playwright-adapter/`

4. **app** (Desktop Application)
   - Swing-based GUI for browser automation
   - Recorder functionality for capturing user interactions
   - Multi-user context management
   - Screenshot and video recording capabilities
   - Location: `/app/`

5. **playwright-delegator**
   - Location: `/playwright-delegator/`

### Technology Stack
- **Language:** Java 8 (for compatibility)
- **Build Tool:** Gradle 8.5
- **WebSocket:** Java-WebSocket 1.5.2
- **JSON:** Gson 2.8.9
- **Testing:** JUnit 5
- **GUI:** Swing (for desktop app)

## Building and Testing

### Prerequisites
- Java 8 JDK (required for building the app module)
- Gradle 8.5+ (wrapper provided)
- Supported browsers with BiDi support:
  - Firefox 91+
  - Chrome 96+
  - Edge 96+
  - Safari (experimental)

### Build Commands

```bash
# Build all modules (excluding app due to Java version constraints)
./gradlew clean build -x :app:build

# Build specific modules
./gradlew :wd4j:build
./gradlew :playwright-adapter:build
./gradlew :playwright-java:build

# Build shadow JARs (fat JARs with dependencies)
./gradlew :wd4j:shadowJar
./gradlew :playwright-adapter:shadowJar
./gradlew :app:shadowJar

# Run tests
./gradlew test

# Run tests for specific module
./gradlew :wd4j:test
```

### Proxy Configuration (Windows)

If behind a corporate proxy with WPAD/PAC configuration:

```bash
# Build with proxy configuration
./gradlew assemble --init-script proxy-init.gradle

# Configure Git proxy (one-time setup)
./configure-git-proxy.ps1
```

For permanent proxy configuration, copy `proxy-init.gradle` to `%USERPROFILE%\.gradle\init.gradle`.

## Code Style and Conventions

### General Guidelines
- **Java Version:** Target Java 8 for maximum compatibility
- **Encoding:** UTF-8 for all source files
- **Package Structure:** Follow existing package hierarchy
  - Core: `de.bund.zrb.*`
  - Playwright API: `com.microsoft.playwright.*`
- **Naming Conventions:**
  - Classes: PascalCase
  - Methods: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Packages: lowercase

### Code Organization
- **Single Responsibility:** Each manager class handles one concern (e.g., `WDBrowserManager`, `WDSessionManager`)
- **Event-Driven:** Use the event dispatcher for browser state changes
- **Type Safety:** Leverage typed responses and commands
- **Interface Segregation:** Keep interfaces focused and minimal

### Testing Guidelines
- Use JUnit 5 for all tests
- Test files should mirror source structure in `src/test/java/`
- Integration tests for end-to-end scenarios
- Unit tests for individual components
- Mock external dependencies where appropriate

## Development Workflow

### Making Changes

1. **Before making changes:**
   - Understand the module you're modifying
   - Check existing tests related to your changes
   - Review relevant README files in the module directories

2. **When modifying code:**
   - Make minimal, surgical changes
   - Preserve existing functionality unless fixing bugs
   - Update related tests
   - Ensure Java 8 compatibility

3. **Testing your changes:**
   - Run tests for the affected module
   - Verify builds succeed
   - Test integration with dependent modules

4. **Documentation:**
   - Update module-level README files if architecture changes
   - Update main README.md for user-facing changes
   - Add inline comments only when necessary for complex logic

### Module Dependencies

```
app
├── depends on: playwright-adapter, wd4j
│
playwright-adapter
├── depends on: playwright-java, wd4j
│
playwright-java
├── depends on: (none - interface definitions only)
│
wd4j
├── depends on: (minimal external dependencies)
```

## Common Development Scenarios

### Adding a New WebDriver BiDi Command

1. Define the command in `/wd4j/src/main/java/de/bund/zrb/command/request/`
2. Define the response in `/wd4j/src/main/java/de/bund/zrb/command/response/`
3. Add manager method if needed in `/wd4j/src/main/java/de/bund/zrb/manager/`
4. Add tests in corresponding test directory
5. Update playwright-adapter if exposing through Playwright API

### Implementing a Playwright API Feature

1. Check if interface exists in `playwright-java`
2. Implement in `playwright-adapter` using wd4j core
3. Map to appropriate WebDriver BiDi commands
4. Add tests for the implementation
5. Document any limitations or differences from original Playwright

### Debugging Tips

- Enable verbose logging for WebSocket communication
- Use browser DevTools to inspect BiDi protocol messages
- Check event dispatcher for proper event routing
- Verify browsing context IDs are correctly tracked

## Known Issues and Limitations

### Build System
- The `app` module requires Java 8 JDK specifically (tools.jar dependency)
- Building with Java 17+ may cause issues with the app module
- Workaround: Build non-app modules separately or use Java 8

### WebDriver BiDi Implementation
- **Messaging Overhead:** Current "Frame" concept needs simplification
  - Events should route directly per browsing context
  - Planned optimization to reduce unnecessary event copying
- **Browser Support:** Primary focus is Firefox; Chromium/Edge/WebKit are in progress
- **Playwright API Coverage:** Not all Playwright options are implemented yet
  - Network intercept/request forwarding is limited
  - Some LaunchOptions and NewContextOptions are incomplete
  - Event API partially compatible

## Resources

- [Main README.md](../readme.md) - Comprehensive project documentation
- [WebDriver BiDi Specification (W3C)](https://w3c.github.io/webdriver-bidi/)
- [Session Management in BiDi](https://w3c.github.io/webdriver-bidi/#session)
- [BiDi Protocol Modules](https://w3c.github.io/webdriver-bidi/#modules)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes following the guidelines above
4. Write/update tests
5. Ensure all builds and tests pass
6. Submit a pull request

For issues and feature requests, use the [GitHub Issue Tracker](https://github.com/Miguel0888/WD4J/issues).

## License

This project is licensed under the MIT License. Note that the Playwright API interfaces may have different licensing.
