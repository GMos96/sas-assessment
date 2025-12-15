package com.example.sas.features.customer.mapper;

import com.example.sas.features.customer.dto.AddressRequest;
import com.example.sas.features.customer.dto.AddressResponse;
import com.example.sas.features.customer.dto.CustomerRequest;
import com.example.sas.features.customer.dto.CustomerResponse;
import com.example.sas.features.customer.entity.Address;
import com.example.sas.features.customer.entity.Customer;
import com.example.sas.features.customer.entity.CustomerHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomerMapper
 * Demonstrates that mapping logic can be tested independently of service layer
 */
class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper();
    }

    @Test
    void toCustomerEntity_validRequest_mapsCorrectly() {
        // Given
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBirthday(LocalDate.of(1990, 1, 15));
        request.setEmail("john.doe@example.com");
        request.setPhone("+1-555-123-4567");

        // When
        Customer customer = mapper.toCustomerEntity(request);

        // Then
        assertNotNull(customer);
        assertEquals("John", customer.getFirstName());
        assertEquals("Doe", customer.getLastName());
        assertEquals(LocalDate.of(1990, 1, 15), customer.getBirthday());
        assertEquals("john.doe@example.com", customer.getEmail());
        assertEquals("+1-555-123-4567", customer.getPhone());
        assertNotNull(customer.getCreatedAt());
        assertNotNull(customer.getUpdatedAt());
    }

    @Test
    void toCustomerEntity_nullRequest_returnsNull() {
        // When
        Customer customer = mapper.toCustomerEntity(null);

        // Then
        assertNull(customer);
    }

    @Test
    void toCustomerResponse_validCustomer_mapsCorrectly() {
        // Given
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setBirthday(LocalDate.of(1990, 1, 15));
        customer.setEmail("john.doe@example.com");
        customer.setPhone("+1-555-123-4567");
        customer.setSsnMasked("XXX-XX-6789");
        customer.setCreatedAt(OffsetDateTime.now());
        customer.setUpdatedAt(OffsetDateTime.now());

        Address address = new Address();
        address.setId(UUID.randomUUID());
        address.setType("HOME");
        address.setStreet("123 Main St");
        address.setCity("New York");
        address.setState("NY");
        address.setPostalCode("10001");
        address.setCountry("USA");

        // When
        CustomerResponse response = mapper.toCustomerResponse(customer, List.of(address));

        // Then
        assertNotNull(response);
        assertEquals(customer.getId(), response.getId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("XXX-XX-6789", response.getSsnMasked());
        assertNotNull(response.getAddresses());
        assertEquals(1, response.getAddresses().size());
        assertEquals("HOME", response.getAddresses().get(0).getType());
    }

    @Test
    void toCustomerResponse_nullCustomer_returnsNull() {
        // When
        CustomerResponse response = mapper.toCustomerResponse(null, List.of());

        // Then
        assertNull(response);
    }

    @Test
    void updateCustomerFromRequest_updatesOnlyNonNullFields() {
        // Given
        Customer existing = new Customer();
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setEmail("john@example.com");
        existing.setPhone("555-1234");

        CustomerRequest update = new CustomerRequest();
        update.setFirstName("Jane"); // Update
        update.setEmail("jane@example.com"); // Update
        // lastName and phone are null - should not be updated

        // When
        mapper.updateCustomerFromRequest(existing, update);

        // Then
        assertEquals("Jane", existing.getFirstName()); // Updated
        assertEquals("Doe", existing.getLastName()); // Unchanged
        assertEquals("jane@example.com", existing.getEmail()); // Updated
        assertEquals("555-1234", existing.getPhone()); // Unchanged
        assertNotNull(existing.getUpdatedAt());
    }

    @Test
    void updateCustomerFromRequest_nullInputs_doesNothing() {
        // Given
        Customer customer = new Customer();
        customer.setFirstName("John");

        // When
        mapper.updateCustomerFromRequest(null, new CustomerRequest());
        mapper.updateCustomerFromRequest(customer, null);

        // Then
        assertEquals("John", customer.getFirstName()); // Unchanged
    }

    @Test
    void toAddressEntity_validRequest_mapsCorrectly() {
        // Given
        UUID customerId = UUID.randomUUID();
        AddressRequest request = new AddressRequest();
        request.setType("HOME");
        request.setStreet("123 Main St");
        request.setCity("New York");
        request.setState("NY");
        request.setPostalCode("10001");
        request.setCountry("USA");

        // When
        Address address = mapper.toAddressEntity(request, customerId);

        // Then
        assertNotNull(address);
        assertEquals(customerId, address.getCustomerId());
        assertEquals("HOME", address.getType());
        assertEquals("123 Main St", address.getStreet());
        assertEquals("New York", address.getCity());
        assertEquals("NY", address.getState());
        assertEquals("10001", address.getPostalCode());
        assertEquals("USA", address.getCountry());
    }

    @Test
    void toAddressEntity_nullRequest_returnsNull() {
        // When
        Address address = mapper.toAddressEntity(null, UUID.randomUUID());

        // Then
        assertNull(address);
    }

    @Test
    void toAddressResponse_validAddress_mapsCorrectly() {
        // Given
        Address address = new Address();
        address.setId(UUID.randomUUID());
        address.setType("WORK");
        address.setStreet("456 Office Blvd");
        address.setCity("Boston");
        address.setState("MA");
        address.setPostalCode("02101");
        address.setCountry("USA");

        // When
        AddressResponse response = mapper.toAddressResponse(address);

        // Then
        assertNotNull(response);
        assertEquals(address.getId(), response.getId());
        assertEquals("WORK", response.getType());
        assertEquals("456 Office Blvd", response.getStreet());
        assertEquals("Boston", response.getCity());
        assertEquals("MA", response.getState());
        assertEquals("02101", response.getPostalCode());
        assertEquals("USA", response.getCountry());
    }

    @Test
    void toAddressResponse_nullAddress_returnsNull() {
        // When
        AddressResponse response = mapper.toAddressResponse(null);

        // Then
        assertNull(response);
    }

    @Test
    void toAddressEntities_validList_mapsAll() {
        // Given
        UUID customerId = UUID.randomUUID();
        AddressRequest req1 = new AddressRequest();
        req1.setType("HOME");
        req1.setStreet("123 Main St");
        req1.setCity("NYC");
        req1.setPostalCode("10001");
        req1.setCountry("USA");

        AddressRequest req2 = new AddressRequest();
        req2.setType("WORK");
        req2.setStreet("456 Office Blvd");
        req2.setCity("Boston");
        req2.setPostalCode("02101");
        req2.setCountry("USA");

        // When
        List<Address> addresses = mapper.toAddressEntities(List.of(req1, req2), customerId);

        // Then
        assertNotNull(addresses);
        assertEquals(2, addresses.size());
        assertEquals("HOME", addresses.get(0).getType());
        assertEquals("WORK", addresses.get(1).getType());
        assertEquals(customerId, addresses.get(0).getCustomerId());
        assertEquals(customerId, addresses.get(1).getCustomerId());
    }

    @Test
    void toAddressEntities_nullList_returnsEmptyList() {
        // When
        List<Address> addresses = mapper.toAddressEntities(null, UUID.randomUUID());

        // Then
        assertNotNull(addresses);
        assertTrue(addresses.isEmpty());
    }

    @Test
    void toCustomerHistory_validCustomer_createsHistoryRecord() {
        // Given
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setBirthday(LocalDate.of(1990, 1, 15));
        customer.setEmail("john@example.com");
        customer.setPhone("555-1234");
        customer.setSsnEncrypted("encrypted");
        customer.setSsnHash("hash");
        customer.setSsnMasked("XXX-XX-6789");
        customer.setVersion(1L);

        // When
        CustomerHistory history = mapper.toCustomerHistory(customer, "CREATED");

        // Then
        assertNotNull(history);
        assertEquals(customer.getId(), history.getCustomerId());
        assertEquals("John", history.getFirstName());
        assertEquals("Doe", history.getLastName());
        assertEquals("CREATED", history.getChangeType());
        assertEquals("encrypted", history.getSsnEncrypted());
        assertEquals("hash", history.getSsnHash());
        assertEquals("XXX-XX-6789", history.getSsnMasked());
        assertEquals(1L, history.getVersion());
        assertNotNull(history.getChangedAt());
    }

    @Test
    void toCustomerHistory_nullCustomer_returnsNull() {
        // When
        CustomerHistory history = mapper.toCustomerHistory(null, "CREATED");

        // Then
        assertNull(history);
    }
}

