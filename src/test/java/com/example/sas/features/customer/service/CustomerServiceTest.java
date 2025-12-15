package com.example.sas.features.customer.service;

import com.example.sas.common.security.abstractions.EncryptionService;
import com.example.sas.common.security.dto.EncryptionResult;
import com.example.sas.features.customer.dto.CustomerRequest;
import com.example.sas.features.customer.dto.CustomerResponse;
import com.example.sas.features.customer.dto.CustomerUpdateRequest;
import com.example.sas.features.customer.entity.Address;
import com.example.sas.features.customer.entity.Customer;
import com.example.sas.features.customer.entity.CustomerHistory;
import com.example.sas.features.customer.exceptions.CustomerNotFoundException;
import com.example.sas.features.customer.exceptions.DuplicateSsnException;
import com.example.sas.features.customer.mapper.CustomerMapper;
import com.example.sas.features.customer.repository.AddressRepository;
import com.example.sas.features.customer.repository.CustomerHistoryRepository;
import com.example.sas.features.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.userdetails.User;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerHistoryRepository customerHistoryRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private EncryptionService encryptionService;

    private final CustomerMapper customerMapper = new CustomerMapper();

    private CustomerService customerService;
    private UUID testCustomerId;
    private User testUser;

    @BeforeEach
    void setUp() {
        this.customerService = new CustomerService(customerRepository, addressRepository, customerHistoryRepository, encryptionService, customerMapper);
        this.testCustomerId = UUID.randomUUID();
        this.testUser = new User("test-user", "password", Collections.emptyList());
    }

    @Test
    void updateCustomer_withConcurrentModification_throwsOptimisticLockingException() {
        Customer existingCustomer = new Customer();
        existingCustomer.setId(testCustomerId);

        when(customerRepository.findById(testCustomerId))
                .thenReturn(Optional.of(existingCustomer));

        when(customerHistoryRepository.save(any(CustomerHistory.class)))
                .thenThrow(new OptimisticLockingFailureException("Concurrent modification detected"));

        assertThatThrownBy(() -> customerService.updateCustomer(testCustomerId, new CustomerUpdateRequest(), testUser))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining("Concurrent modification detected");
    }

    @Test
    void createCustomer_withSsn_encryptsAndPersistsCustomerAndHistory() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setSsn("123-45-6789");

        String ssnHash = "hashed-ssn";

        EncryptionResult encryptionResult = new EncryptionResult("cipher", "ivBase64", "key-id");

        Customer savedCustomer = new Customer();
        savedCustomer.setId(testCustomerId);

        CustomerHistory history = new CustomerHistory();
        history.setCustomerId(testCustomerId);

        when(encryptionService.hmacSha256("123-45-6789")).thenReturn(ssnHash);
        when(customerRepository.findBySsnHash(ssnHash)).thenReturn(Optional.empty());
        when(encryptionService.encrypt("123-45-6789".getBytes())).thenReturn(encryptionResult);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerHistoryRepository.save(any(CustomerHistory.class))).thenReturn(history);

        CustomerResponse result = customerService.createCustomer(request, testUser);

        assertThat(result).isNotNull();
        verify(customerRepository).findBySsnHash(ssnHash);
        verify(encryptionService).encrypt("123-45-6789".getBytes());
        verify(customerHistoryRepository).save(any(CustomerHistory.class));
    }

    @Test
    void createCustomer_withoutSsn_skipsEncryptionAndDuplicateCheck() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setSsn(null);

        Customer savedCustomer = new Customer();
        savedCustomer.setId(testCustomerId);

        CustomerHistory history = new CustomerHistory();
        history.setCustomerId(testCustomerId);

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerHistoryRepository.save(any(CustomerHistory.class))).thenReturn(history);

        CustomerResponse result = customerService.createCustomer(request, testUser);

        assertThat(result).isNotNull();
        verify(encryptionService, never()).hmacSha256(any());
        verify(encryptionService, never()).encrypt(any());
        verify(customerRepository, never()).findBySsnHash(any());
    }

    @Test
    void createCustomer_withDuplicateSsn_throwsDuplicateSsnException() {
        CustomerRequest request = new CustomerRequest();
        request.setSsn("123-45-6789");

        String ssnHash = "hashed-ssn";
        Customer existingCustomer = new Customer();

        when(encryptionService.hmacSha256("123-45-6789")).thenReturn(ssnHash);
        when(customerRepository.findBySsnHash(ssnHash)).thenReturn(Optional.of(existingCustomer));

        assertThatThrownBy(() -> customerService.createCustomer(request, testUser))
                .isInstanceOf(DuplicateSsnException.class);

        verify(encryptionService, never()).encrypt(any());
        verify(customerRepository, never()).save(any(Customer.class));
        verify(addressRepository, never()).save(any(Address.class));
        verify(customerHistoryRepository, never()).save(any(CustomerHistory.class));
    }

    @Test
    void getCustomer_existingId_returnsMappedResponseWithAddresses() {
        Customer customer = new Customer();
        customer.setId(testCustomerId);

        Address address = new Address();
        address.setCustomerId(testCustomerId);
        List<Address> addresses = List.of(address);

        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(customer));
        when(addressRepository.findAllByCustomerId(testCustomerId)).thenReturn(addresses);

        CustomerResponse result = customerService.getCustomer(testCustomerId);

        assertThat(result).isNotNull();
        assertThat(result.getAddresses()).hasSize(1);
    }

    @Test
    void getCustomer_nonExistingId_throwsCustomerNotFoundException() {
        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(testCustomerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found with ID");

        verify(addressRepository, never()).findAllByCustomerId(any());
    }

    @Test
    void updateCustomer_existingCustomer_updatesFieldsAndHistory() {
        Customer existingCustomer = new Customer();
        existingCustomer.setId(testCustomerId);

        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setSsn(null);
        request.setAddresses(null);

        CustomerHistory history = new CustomerHistory();
        history.setCustomerId(testCustomerId);

        Customer savedCustomer = new Customer();
        savedCustomer.setId(testCustomerId);

        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(existingCustomer));
        when(customerHistoryRepository.save(any(CustomerHistory.class))).thenReturn(history);
        when(addressRepository.findAllByCustomerId(testCustomerId)).thenReturn(Collections.emptyList());
        when(customerRepository.save(existingCustomer)).thenReturn(savedCustomer);

        CustomerResponse result = customerService.updateCustomer(testCustomerId, request, testUser);

        assertThat(result).isNotNull();
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void updateCustomer_withNewSsn_updatesEncryptionFields() {
        Customer existingCustomer = new Customer();
        existingCustomer.setId(testCustomerId);

        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setSsn("987-65-4321");
        request.setAddresses(null);

        CustomerHistory history = new CustomerHistory();
        history.setCustomerId(testCustomerId);

        EncryptionResult encryptionResult = new EncryptionResult("cipher", "ivBase64", "key-id");
        String ssnHash = "new-ssn-hash";

        Customer savedCustomer = new Customer();
        savedCustomer.setId(testCustomerId);

        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(existingCustomer));
        when(customerHistoryRepository.save(any(CustomerHistory.class))).thenReturn(history);
        when(encryptionService.hmacSha256("987-65-4321")).thenReturn(ssnHash);
        when(encryptionService.encrypt("987-65-4321".getBytes())).thenReturn(encryptionResult);
        when(addressRepository.findAllByCustomerId(testCustomerId)).thenReturn(Collections.emptyList());
        when(customerRepository.save(existingCustomer)).thenReturn(savedCustomer);

        CustomerResponse result = customerService.updateCustomer(testCustomerId, request, testUser);

        assertThat(result).isNotNull();
        verify(encryptionService).hmacSha256("987-65-4321");
        verify(encryptionService).encrypt("987-65-4321".getBytes());
    }

    @Test
    void updateCustomer_withNewAddresses_replacesOldAddresses() {
        Customer existingCustomer = new Customer();
        existingCustomer.setId(testCustomerId);

        CustomerUpdateRequest request = new CustomerUpdateRequest();

        Address oldAddress1 = new Address();
        oldAddress1.setId(UUID.randomUUID());
        Address oldAddress2 = new Address();
        oldAddress2.setId(UUID.randomUUID());

        List<Address> oldAddresses = List.of(oldAddress1, oldAddress2);

        CustomerHistory history = new CustomerHistory();
        history.setCustomerId(testCustomerId);

        Customer savedCustomer = new Customer();
        savedCustomer.setId(testCustomerId);

        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.of(existingCustomer));
        when(customerHistoryRepository.save(any(CustomerHistory.class))).thenReturn(history);
        when(addressRepository.findAllByCustomerId(testCustomerId)).thenReturn(oldAddresses);
        when(customerRepository.save(existingCustomer)).thenReturn(savedCustomer);

        CustomerResponse result = customerService.updateCustomer(testCustomerId, request, testUser);

        assertThat(result).isNotNull();
        verify(addressRepository).deleteById(oldAddress1.getId());
        verify(addressRepository).deleteById(oldAddress2.getId());
    }

    @Test
    void updateCustomer_nonExistingId_throwsCustomerNotFoundException() {
        CustomerUpdateRequest request = new CustomerUpdateRequest();

        when(customerRepository.findById(testCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(testCustomerId, request, testUser))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found with ID");

        verify(customerHistoryRepository, never()).save(any(CustomerHistory.class));
        verify(addressRepository, never()).findAllByCustomerId(any());
        verify(encryptionService, never()).hmacSha256(any());
    }

    @Test
    void getHistory_existingCustomerWithHistory_returnsOrderedList() {
        CustomerHistory history1 = new CustomerHistory();
        history1.setCustomerId(testCustomerId);
        history1.setChangedAt(OffsetDateTime.now());

        CustomerHistory history2 = new CustomerHistory();
        history2.setCustomerId(testCustomerId);
        history2.setChangedAt(OffsetDateTime.now().minusDays(1));

        List<CustomerHistory> histories = List.of(history1, history2);

        when(customerHistoryRepository.findAllByCustomerIdOrderByChangedAtDesc(testCustomerId)).thenReturn(histories);

        List<CustomerHistory> result = customerService.getHistory(testCustomerId);

        assertThat(result).containsExactlyElementsOf(histories);
    }

    @Test
    void getHistory_noHistory_returnsEmptyList() {
        when(customerHistoryRepository.findAllByCustomerIdOrderByChangedAtDesc(testCustomerId)).thenReturn(Collections.emptyList());

        List<CustomerHistory> result = customerService.getHistory(testCustomerId);

        assertThat(result).isEmpty();
    }
}
