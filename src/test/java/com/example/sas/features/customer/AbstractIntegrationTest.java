package com.example.sas.features.customer;

import com.example.sas.features.customer.dto.CustomerRequest;
import com.example.sas.features.customer.dto.AddressRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.LocalDate;
import java.util.List;

/**
 * Base class for integration tests using TestContainers.
 * Provides a PostgreSQL container and helper methods for test data setup.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    protected CustomerRequest buildValidCustomerRequest(String firstName, String lastName, String ssn) {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setBirthday(LocalDate.of(1990, 1, 15));
        request.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        request.setPhone("+1-555-123-4567");
        request.setSsn(ssn);

        AddressRequest address = new AddressRequest();
        address.setType("HOME");
        address.setStreet("123 Main St");
        address.setCity("New York");
        address.setState("NY");
        address.setPostalCode("10001");
        address.setCountry("USA");

        request.setAddresses(List.of(address));
        return request;
    }
}
