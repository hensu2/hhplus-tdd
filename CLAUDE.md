# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TDD practice project for implementing a point management system with concurrent access control. The project follows Test-Driven Development principles with a focus on Red-Green-Refactor cycles.

## Build & Development Commands

### Building and Running
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Create bootable JAR
./gradlew bootJar
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "ClassName"

# Run a specific test method
./gradlew test --tests "ClassName.methodName"

# Run tests with JaCoCo code coverage
./gradlew test jacocoTestReport
```

Note: Tests are configured with `ignoreFailures = true` in build.gradle.kts, so the build continues even if tests fail.

## Architecture

### Core Domain: Point Management (`io.hhplus.tdd.point`)

The system implements four main features:
- **Point Query**: Retrieve user point balance
- **Point Charge**: Add points to user account
- **Point Use**: Deduct points from user account
- **History Query**: Retrieve point transaction history

Domain models use Java records:
- `UserPoint`: Represents user point balance (id, point, updateMillis)
- `PointHistory`: Records point transactions (id, userId, amount, type, updateMillis)
- `TransactionType`: Enum for CHARGE/USE operations

### Data Layer (`io.hhplus.tdd.database`)

The project uses in-memory table implementations (DO NOT modify these classes):
- `UserPointTable`: Thread-safe user point storage with simulated latency (200-500ms)
- `PointHistoryTable`: Transaction history storage with simulated latency (300ms)

Both tables include deliberate throttling via `Thread.sleep()` to simulate real database latency and expose concurrency issues.

### Layered Architecture

Implement the following layers when developing features:
1. **Controller** (`PointController`): REST endpoints already defined with TODO markers
2. **Service**: Business logic layer (to be implemented)
3. **Repository/Table**: Data access via provided table classes

### Concurrency Control

Concurrent access to shared point data is a critical requirement. When implementing:
- Identify critical sections where race conditions can occur
- Implement appropriate synchronization mechanisms
- Document the chosen concurrency control strategy in README.md
- Write tests that verify concurrent access safety

The table classes' built-in latency helps expose race conditions during testing.

## TDD Workflow

Follow strict TDD principles:
1. Write failing test first (Red)
2. Implement minimal code to pass test (Green)
3. Refactor while keeping tests green (Refactor)

Test coverage should include:
- Unit tests for each service method
- Integration tests for controller endpoints
- Edge cases: negative amounts, insufficient balance, non-existent users
- Concurrency tests: multiple threads accessing same user's points

## Key Constraints

- DO NOT modify `UserPointTable` and `PointHistoryTable` classes
- Use only the public APIs of table classes
- Java 17 source compatibility
- Spring Boot 3.2.0 framework
- Lombok for reducing boilerplate

## Pull Request Guidelines

PR titles must follow: `[STEP0X] Name`

Core checklist items to verify:
- All four features implemented (query/charge/use/history)
- Unit tests for each feature
- Exception handling tests (insufficient balance, etc.)
- Integration tests
- Concurrency control implementation
- Concurrency strategy documented in README.md
