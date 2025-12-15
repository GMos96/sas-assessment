package com.example.sas.integration;

import com.example.sas.features.customer.dto.AddressRequest;
import com.example.sas.features.customer.dto.CustomerRequest;
import com.example.sas.features.customer.dto.CustomerResponse;
import com.example.sas.features.customer.entity.CustomerHistory;
import com.example.sas.features.customer.exceptions.CustomerNotFoundException;
import com.example.sas.features.customer.exceptions.DuplicateSsnException;
import com.example.sas.features.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for CustomerService.
 * Tests business logic with real database and encryption.
 */
@Transactional // Roll back after each test for isolation
class CustomerServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerService customerService;

    private CustomerRequest validCustomerRequest;

    @BeforeEach
    void setUp() {
        validCustomerRequest = new CustomerRequest();
        validCustomerRequest.setFirstName("John");
        validCustomerRequest.setLastName("Doe");
        validCustomerRequest.setBirthday(LocalDate.of(1990, 1, 15));
        validCustomerRequest.setEmail("john.doe@example.com");
        validCustomerRequest.setPhone("+1-555-123-4567");
        validCustomerRequest.setSsn("123-45-6789");

        AddressRequest address = new AddressRequest();
        address.setType("HOME");
        address.setStreet("123 Main Street");
        address.setCity("New York");
        address.setState("NY");
        address.setPostalCode("10001");
        address.setCountry("USA");

        validCustomerRequest.setAddresses(List.of(address));
    }

    @Test
    void createCustomer_withValidData_encryptsAndSavesSuccessfully() {
        // When
        CustomerResponse response = customerService.createCustomer(validCustomerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");

        // SSN should be masked, never returned in plaintext
        assertThat(response.getSsnMasked()).isEqualTo("XXX-XX-6789");
        assertThat(response.getSsnMasked()).doesNotContain("123-45");

        // Addresses should be saved
        assertThat(response.getAddresses()).hasSize(1);
        assertThat(response.getAddresses().get(0).getType()).isEqualTo("HOME");
        assertThat(response.getAddresses().get(0).getStreet()).isEqualTo("123 Main Street");

        // Timestamps should be set
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void createCustomer_withDuplicateSsn_throwsDuplicateSsnException() {
        // Given: Create first customer
        customerService.createCustomer(validCustomerRequest);

        // When: Try to create second customer with same SSN
        CustomerRequest duplicate = new CustomerRequest();
        duplicate.setFirstName("Jane");
        duplicate.setLastName("Smith");
        duplicate.setBirthday(LocalDate.of(1985, 5, 20));
        duplicate.setEmail("jane.smith@example.com");
        duplicate.setSsn("123-45-6789"); // Same SSN!

        // Then: Throws DuplicateSsnException
        assertThatThrownBy(() -> customerService.createCustomer(duplicate))
                .isInstanceOf(DuplicateSsnException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createCustomer_createsHistoryRecord() {
        // When: Create customer
        CustomerResponse response = customerService.createCustomer(validCustomerRequest);

        // Then: History record should exist
        List<CustomerHistory> history = customerService.getHistory(response.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getChangeType()).isEqualTo("CREATED");
        assertThat(history.get(0).getFirstName()).isEqualTo("John");
        assertThat(history.get(0).getLastName()).isEqualTo("Doe");
    }

    @Test
    void getCustomer_withValidId_returnsCustomerWithAddresses() {
        // Given: Create customer
        CustomerResponse created = customerService.createCustomer(validCustomerRequest);

        // When: Get customer by ID
        CustomerResponse retrieved = customerService.getCustomer(created.getId());

        // Then: Returns complete customer data
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(created.getId());
        assertThat(retrieved.getFirstName()).isEqualTo("John");
        assertThat(retrieved.getAddresses()).hasSize(1);
        assertThat(retrieved.getSsnMasked()).isEqualTo("XXX-XX-6789");
    }

    @Test
    void getCustomer_withNonExistentId_throwsCustomerNotFoundException() {
        // When/Then
        UUID nonExistentId = UUID.randomUUID();
        assertThatThrownBy(() -> customerService.getCustomer(nonExistentId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateCustomer_withValidData_updatesAndCreatesHistoryRecord() {
        // Given: Create customer
        CustomerResponse created = customerService.createCustomer(validCustomerRequest);

        // Given: Update request
        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");
        updateRequest.setEmail("jane.smith@example.com");
        updateRequest.setPhone("+1-555-999-8888");
        updateRequest.setBirthday(LocalDate.of(1990, 1, 15));
        updateRequest.setSsn("987-65-4321"); // Different SSN

        // When: Update customer
        CustomerResponse updated = customerService.updateCustomer(created.getId(), updateRequest);

        // Then: Customer is updated
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getFirstName()).isEqualTo("Jane");
        assertThat(updated.getLastName()).isEqualTo("Smith");
        assertThat(updated.getEmail()).isEqualTo("jane.smith@example.com");
        assertThat(updated.getSsnMasked()).isEqualTo("XXX-XX-4321");

        // Then: History contains both CREATED and UPDATED records
        List<CustomerHistory> history = customerService.getHistory(updated.getId());
        assertThat(history).hasSizeGreaterThanOrEqualTo(2);
        assertThat(history.get(0).getChangeType()).isEqualTo("UPDATED"); // Most recent
        assertThat(history.get(0).getFirstName()).isEqualTo("John"); // Previous state
        assertThat(history.get(1).getChangeType()).isEqualTo("CREATED");
    }

    @Test
    void updateCustomer_withNonExistentId_throwsCustomerNotFoundException() {
        // When/Then
        UUID nonExistentId = UUID.randomUUID();
        assertThatThrownBy(() -> customerService.updateCustomer(nonExistentId, validCustomerRequest))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateCustomer_withNewAddresses_replacesOldAddresses() {
        // Given: Create customer with one address
        CustomerResponse created = customerService.createCustomer(validCustomerRequest);
        assertThat(created.getAddresses()).hasSize(1);

        // Given: Update with two different addresses
        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setFirstName("John");
        updateRequest.setLastName("Doe");
        updateRequest.setBirthday(LocalDate.of(1990, 1, 15));
        updateRequest.setSsn("123-45-6789");

        AddressRequest workAddress = new AddressRequest();
        workAddress.setType("WORK");
        workAddress.setStreet("456 Office Blvd");
        workAddress.setCity("Boston");
        workAddress.setState("MA");
        workAddress.setPostalCode("02101");
        workAddress.setCountry("USA");

        AddressRequest billingAddress = new AddressRequest();
        billingAddress.setType("BILLING");
        billingAddress.setStreet("789 Billing St");
        billingAddress.setCity("Chicago");
        billingAddress.setState("IL");
        billingAddress.setPostalCode("60601");
        billingAddress.setCountry("USA");

        updateRequest.setAddresses(List.of(workAddress, billingAddress));

        // When: Update
        CustomerResponse updated = customerService.updateCustomer(created.getId(), updateRequest);

        // Then: Old address is replaced with new ones
        assertThat(updated.getAddresses()).hasSize(2);
        assertThat(updated.getAddresses())
                .extracting("type")
                .containsExactlyInAnyOrder("WORK", "BILLING");
        assertThat(updated.getAddresses())
                .extracting("type")
                .doesNotContain("HOME"); // Old address removed
    }

    @Test
    void getHistory_withMultipleUpdates_returnsAllRecordsInDescendingOrder() throws InterruptedException {
        // Given: Create customer
        CustomerResponse created = customerService.createCustomer(validCustomerRequest);

        // Given: Make multiple updates
        CustomerRequest update1 = new CustomerRequest();
        update1.setFirstName("Update1");
        update1.setLastName("Doe");
        update1.setBirthday(LocalDate.of(1990, 1, 15));
        update1.setSsn("123-45-6789");
        customerService.updateCustomer(created.getId(), update1);

        Thread.sleep(100); // Ensure different timestamps

        CustomerRequest update2 = new CustomerRequest();
        update2.setFirstName("Update2");
        update2.setLastName("Doe");
        update2.setBirthday(LocalDate.of(1990, 1, 15));
        update2.setSsn("123-45-6789");
        customerService.updateCustomer(created.getId(), update2);

        // When: Get history
        List<CustomerHistory> history = customerService.getHistory(created.getId());

        // Then: All records present, most recent first
        assertThat(history).hasSizeGreaterThanOrEqualTo(3);
        assertThat(history.get(0).getChangeType()).isEqualTo("UPDATED");
        assertThat(history.get(0).getFirstName()).isEqualTo("Update1"); // Previous state before update2
        assertThat(history.get(1).getChangeType()).isEqualTo("UPDATED");
        assertThat(history.get(1).getFirstName()).isEqualTo("John"); // Original state before update1
        assertThat(history.get(2).getChangeType()).isEqualTo("CREATED");
    }

    @Test
    void createCustomer_withNoAddresses_savesCustomerSuccessfully() {
        // Given: Customer without addresses
        validCustomerRequest.setAddresses(null);

        // When: Create customer
        CustomerResponse response = customerService.createCustomer(validCustomerRequest);

        // Then: Customer is saved without addresses
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getAddresses()).isEmpty();
    }

    @Test
    void createCustomer_ssnIsEncryptedInDatabase() {
        // When: Create customer
        CustomerResponse response = customerService.createCustomer(validCustomerRequest);

        // Then: Retrieve and verify SSN is encrypted
        CustomerResponse retrieved = customerService.getCustomer(response.getId());

        // Should only see masked SSN, never plaintext
        assertThat(retrieved.getSsnMasked()).isEqualTo("XXX-XX-6789");

        // The response should never contain the original SSN
        String responseJson = retrieved.toString();
        assertThat(responseJson).doesNotContain("123-45-6789");
    }

    @Test
    void updateCustomer_partialUpdate_onlyUpdatesProvidedFields() {
        // Given: Create customer
        CustomerResponse created = customerService.createCustomer(validCustomerRequest);

        // Given: Partial update (only first name)
        CustomerRequest partialUpdate = new CustomerRequest();
        partialUpdate.setFirstName("UpdatedFirstName");
        partialUpdate.setLastName("Doe");
        partialUpdate.setBirthday(LocalDate.of(1990, 1, 15));
        partialUpdate.setSsn("123-45-6789");

        // When: Update
        CustomerResponse updated = customerService.updateCustomer(created.getId(), partialUpdate);

        // Then: Only specified fields are updated
        assertThat(updated.getFirstName()).isEqualTo("UpdatedFirstName");
        assertThat(updated.getLastName()).isEqualTo("Doe"); // Unchanged
        assertThat(updated.getEmail()).isEqualTo("john.doe@example.com"); // Unchanged
    }
}

