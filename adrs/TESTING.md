# ADR: Testing Strategy

## Status
Accepted

## Context
The SAS Assessment project is a Spring Boot REST API for managing customer records in a banking application with sensitive data (SSNs). It requires comprehensive test coverage to ensure:
- Data integrity and correctness
- Security of encryption mechanisms
- API contract compliance
- Database integration reliability

## Decision
We have adopted a **multi-layered testing strategy** combining:

1. **Unit Testing with Mocks**
   - Framework: JUnit 5 with Mockito
   - Scope: Testing individual components in isolation
   - Key tests: `CustomerRequestValidationTest`, `CustomerMapperTest`, `CustomerServicePaginationTest`
   - Benefits: Fast execution, no external dependencies

2. **Integration Testing with TestContainers**
   - Framework: Spring Boot Test + TestContainers
   - Database: PostgreSQL 16 container
   - Scope: Testing full application context with real database
   - Base class: `AbstractIntegrationTest` provides reusable setup
   - Key tests: `CustomerControllerPaginationTest`
   - Benefits: Tests realistic scenarios without compromising local environment
   - TBD: Currently not enabled

3. **Test Configuration**
   - Separate profiles: `application-test.properties` for test-specific settings
   - JUnit Platform for test execution
   - JVM heap allocation: 1GB for test execution
   - Test logging: Full exception format with event tracking (passed, skipped, failed)

## Rationale

- **TestContainers approach** allows us to test against the same PostgreSQL version used in production (v16) without manual Docker setup
- **Separate test profile** ensures tests don't interfere with development database
- **Combination of unit and integration tests** provides balance between speed (unit tests) and confidence (integration tests)
- **Mock-based unit tests** for service layer pagination logic enable fast feedback without container overhead
- **Spring Boot Test with @SpringBootTest** ensures full Spring context is loaded for integration tests, validating security config, validation annotations, and API contracts

## Consequences

**Positive:**
- High confidence in code correctness
- Tests serve as documentation of expected behavior
- Fast feedback from unit tests during development
- Full integration testing prevents surprises in production

**Negative:**
- Integration tests are slower due to container startup
- Requires Docker/TestContainers dependency
- Test maintenance overhead as application grows

## Implementation Details

### Test Types

| Test Type | Framework | Use Case | Example |
|-----------|-----------|----------|---------|
| Unit | JUnit 5 + Mockito | Component behavior in isolation | Validation, business logic |
| Integration | Spring Boot Test + TestContainers | Full application flow with database | API endpoints, database operations |
| Validation | Jakarta Validation API | Input validation constraints | SSN format, email format |

### Key Test Classes

- **AbstractIntegrationTest**: Base class providing PostgreSQL container and helper methods
- **CustomerRequestValidationTest**: Bean validation annotations on request DTOs
- **CustomerServicePaginationTest**: Cursor-based pagination logic with mocks
- **CustomerMapperTest**: Entity-to-DTO mapping logic
- **CustomerControllerPaginationTest**: Full API endpoint integration tests

## References
- JUnit 5: https://junit.org/junit5/
- Mockito: https://site.mockito.org/
- TestContainers: https://www.testcontainers.org/
- Spring Boot Test: https://spring.io/guides/gs/testing-web/

