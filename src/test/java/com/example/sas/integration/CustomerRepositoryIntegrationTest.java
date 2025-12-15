package com.example.sas.integration;

import com.example.sas.features.customer.entity.Customer;
import com.example.sas.features.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CustomerRepository.
 * Tests database operations with real PostgreSQL via TestContainers.
 */
class CustomerRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void saveCustomer_withValidData_persistsSuccessfully() {
        // Given
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setBirthday(LocalDate.of(1990, 1, 15));
        customer.setEmail("john.doe@example.com");
        customer.setPhone("+1-555-123-4567");
        customer.setSsnHash("test-hash-123");
        customer.setSsnEncrypted("encrypted-data");
        customer.setSsnMasked("XXX-XX-6789");
        customer.setCreatedAt(OffsetDateTime.now());
        customer.setUpdatedAt(OffsetDateTime.now());

        // When
        Customer saved = customerRepository.save(customer);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(saved.getSsnHash()).isEqualTo("test-hash-123");
        assertThat(saved.getSsnMasked()).isEqualTo("XXX-XX-6789");
        assertThat(saved.getVersion()).isEqualTo(0L); // Optimistic locking version
    }

    @Test
    void findById_existingCustomer_returnsCustomer() {
        // Given: Save a customer
        Customer customer = createTestCustomer("Jane", "Smith");
        Customer saved = customerRepository.save(customer);

        // When: Find by ID
        Optional<Customer> found = customerRepository.findById(saved.getId());

        // Then: Customer is found
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Jane");
        assertThat(found.get().getLastName()).isEqualTo("Smith");
    }

    @Test
    void findById_nonExistentCustomer_returnsEmpty() {
        // When: Find by non-existent ID
        Optional<Customer> found = customerRepository.findById(java.util.UUID.randomUUID());

        // Then: Returns empty
        assertThat(found).isEmpty();
    }

    @Test
    void findBySsnHash_existingCustomer_returnsCustomer() {
        // Given: Save a customer with specific SSN hash
        Customer customer = createTestCustomer("Alice", "Johnson");
        customer.setSsnHash("unique-hash-456");
        Customer saved = customerRepository.save(customer);

        // When: Find by SSN hash
        Optional<Customer> found = customerRepository.findBySsnHash("unique-hash-456");

        // Then: Customer is found
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getFirstName()).isEqualTo("Alice");
    }

    @Test
    void findBySsnHash_nonExistentHash_returnsEmpty() {
        // When: Find by non-existent hash
        Optional<Customer> found = customerRepository.findBySsnHash("non-existent-hash");

        // Then: Returns empty
        assertThat(found).isEmpty();
    }

    @Test
    void updateCustomer_withOptimisticLocking_incrementsVersion() {
        // Given: Save a customer
        Customer customer = createTestCustomer("Bob", "Wilson");
        Customer saved = customerRepository.save(customer);
        Long originalVersion = saved.getVersion();

        // When: Update the customer
        saved.setEmail("bob.wilson.updated@example.com");
        saved.setUpdatedAt(OffsetDateTime.now());
        Customer updated = customerRepository.save(saved);

        // Then: Version is incremented
        assertThat(updated.getVersion()).isGreaterThan(originalVersion);
        assertThat(updated.getEmail()).isEqualTo("bob.wilson.updated@example.com");
    }

    @Test
    void deleteCustomer_existingCustomer_removesFromDatabase() {
        // Given: Save a customer
        Customer customer = createTestCustomer("Charlie", "Brown");
        Customer saved = customerRepository.save(customer);

        // When: Delete the customer
        customerRepository.deleteById(saved.getId());

        // Then: Customer is no longer found
        Optional<Customer> found = customerRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void saveCustomer_withSoftDelete_flagSetsCorrectly() {
        // Given: Customer marked as deleted
        Customer customer = createTestCustomer("David", "Lee");
        customer.setDeleted(true);

        // When: Save
        Customer saved = customerRepository.save(customer);

        // Then: Deleted flag is persisted
        Optional<Customer> found = customerRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isDeleted()).isTrue();
    }

    @Test
    void saveCustomer_withAllFields_persistsAllData() {
        // Given: Customer with all fields populated
        Customer customer = new Customer();
        customer.setFirstName("Emma");
        customer.setLastName("Davis");
        customer.setBirthday(LocalDate.of(1985, 6, 15));
        customer.setEmail("emma.davis@example.com");
        customer.setPhone("+1-555-987-6543");
        customer.setSsnHash("full-hash-789");
        customer.setSsnEncrypted("fully-encrypted-data");
        customer.setSsnEncryptedIv("test-iv".getBytes());
        customer.setSsnEncryptionKeyId("key-id-123");
        customer.setSsnMasked("XXX-XX-1234");
        customer.setCreatedAt(OffsetDateTime.now());
        customer.setUpdatedAt(OffsetDateTime.now());
        customer.setDeleted(false);

        // When: Save
        Customer saved = customerRepository.save(customer);

        // Then: All fields are persisted
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Emma");
        assertThat(saved.getLastName()).isEqualTo("Davis");
        assertThat(saved.getBirthday()).isEqualTo(LocalDate.of(1985, 6, 15));
        assertThat(saved.getEmail()).isEqualTo("emma.davis@example.com");
        assertThat(saved.getPhone()).isEqualTo("+1-555-987-6543");
        assertThat(saved.getSsnHash()).isEqualTo("full-hash-789");
        assertThat(saved.getSsnEncrypted()).isEqualTo("fully-encrypted-data");
        assertThat(saved.getSsnEncryptedIv()).isEqualTo("test-iv".getBytes());
        assertThat(saved.getSsnEncryptionKeyId()).isEqualTo("key-id-123");
        assertThat(saved.getSsnMasked()).isEqualTo("XXX-XX-1234");
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void saveCustomer_withUniqueConstraintViolation_throwsException() {
        // Given: Save first customer with SSN hash
        Customer first = createTestCustomer("First", "Customer");
        first.setSsnHash("duplicate-hash");
        customerRepository.save(first);

        // When: Try to save second customer with same SSN hash
        Customer second = createTestCustomer("Second", "Customer");
        second.setSsnHash("duplicate-hash");

        // Then: Throws exception (duplicate key violation)
        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> customerRepository.save(second)
        );
    }

    // Helper method to create test customer
    private Customer createTestCustomer(String firstName, String lastName) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setBirthday(LocalDate.of(1990, 1, 1));
        customer.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        customer.setPhone("+1-555-000-0000");
        customer.setSsnHash("hash-" + firstName + "-" + lastName);
        customer.setSsnEncrypted("encrypted");
        customer.setSsnMasked("XXX-XX-0000");
        customer.setCreatedAt(OffsetDateTime.now());
        customer.setUpdatedAt(OffsetDateTime.now());
        return customer;
    }
}

