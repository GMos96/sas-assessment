package com.example.sas.features.customer.dto;

import com.example.sas.features.customer.dto.CustomerRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomerRequest validation
 */
class CustomerRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validCustomerRequest_shouldPassValidation() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBirthday(LocalDate.of(1990, 1, 15));
        request.setEmail("john.doe@example.com");
        request.setPhone("+1-555-123-4567");
        request.setSsn("123-45-6789");

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void missingFirstName_shouldFailValidation() {
        CustomerRequest request = new CustomerRequest();
        request.setLastName("Doe");
        request.setBirthday(LocalDate.of(1990, 1, 15));
        request.setSsn("123-45-6789");

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Missing first name should fail validation");
        assertEquals(1, violations.size());

        ConstraintViolation<CustomerRequest> violation = violations.iterator().next();
        assertEquals("firstName", violation.getPropertyPath().toString());
        assertEquals("First name is required", violation.getMessage());
    }

    @Test
    void invalidSsnFormat_shouldFailValidation() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBirthday(LocalDate.of(1990, 1, 15));
        request.setSsn("123456789"); // Missing hyphens

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Invalid SSN format should fail validation");

        ConstraintViolation<CustomerRequest> violation = violations.stream()
            .filter(v -> v.getPropertyPath().toString().equals("ssn"))
            .findFirst()
            .orElse(null);

        assertNotNull(violation);
        assertEquals("SSN must be in format XXX-XX-XXXX", violation.getMessage());
    }

    @Test
    void invalidEmail_shouldFailValidation() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBirthday(LocalDate.of(1990, 1, 15));
        request.setEmail("not-an-email");
        request.setSsn("123-45-6789");

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Invalid email should fail validation");

        ConstraintViolation<CustomerRequest> violation = violations.stream()
            .filter(v -> v.getPropertyPath().toString().equals("email"))
            .findFirst()
            .orElse(null);

        assertNotNull(violation);
        assertEquals("Email must be valid", violation.getMessage());
    }

    @Test
    void futureBirthday_shouldFailValidation() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBirthday(LocalDate.now().plusDays(1)); // Future date
        request.setSsn("123-45-6789");

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Future birthday should fail validation");

        ConstraintViolation<CustomerRequest> violation = violations.stream()
            .filter(v -> v.getPropertyPath().toString().equals("birthday"))
            .findFirst()
            .orElse(null);

        assertNotNull(violation);
        assertEquals("Birthday must be in the past", violation.getMessage());
    }

    @Test
    void firstNameTooLong_shouldFailValidation() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("A".repeat(101)); // 101 characters
        request.setLastName("Doe");
        request.setBirthday(LocalDate.of(1990, 1, 15));
        request.setSsn("123-45-6789");

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "First name exceeding max length should fail validation");

        ConstraintViolation<CustomerRequest> violation = violations.stream()
            .filter(v -> v.getPropertyPath().toString().equals("firstName"))
            .findFirst()
            .orElse(null);

        assertNotNull(violation);
        assertTrue(violation.getMessage().contains("between 1 and 100"));
    }

    @Test
    void invalidPhoneNumber_shouldFailValidation() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBirthday(LocalDate.of(1990, 1, 15));
        request.setPhone("abc"); // Invalid phone
        request.setSsn("123-45-6789");

        Set<ConstraintViolation<CustomerRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Invalid phone should fail validation");

        ConstraintViolation<CustomerRequest> violation = violations.stream()
            .filter(v -> v.getPropertyPath().toString().equals("phone"))
            .findFirst()
            .orElse(null);

        assertNotNull(violation);
        assertTrue(violation.getMessage().contains("Phone number must be valid"));
    }
}

