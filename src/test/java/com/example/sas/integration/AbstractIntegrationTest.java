package com.example.sas.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base configuration for integration tests using TestContainers.
 * All integration tests should extend this class to get:
 * - Real PostgreSQL database in Docker
 * - Automatic Flyway migrations
 * - Spring Boot context with all beans
 * - Test profile configuration
 *
 * The container is shared across all tests in the same JVM for performance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /**
     * Shared PostgreSQL container for all integration tests.
     * Using @ServiceConnection automatically configures Spring Boot datasource.
     * The container is started automatically by @Testcontainers and @Container.
     */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
}

