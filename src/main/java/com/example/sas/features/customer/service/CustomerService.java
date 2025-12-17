package com.example.sas.features.customer.service;

import com.example.sas.common.pagination.CursorPage;
import com.example.sas.common.pagination.PaginationCursor;
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
import com.example.sas.features.customer.repository.CustomerHistoryRepository;
import com.example.sas.features.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final AddressService addressService;
    private final CustomerHistoryRepository customerHistoryRepository;
    private final EncryptionService encryptionService;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository,
                           AddressService addressService,
                           CustomerHistoryRepository customerHistoryRepository,
                           EncryptionService encryptionService,
                           CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.addressService = addressService;
        this.customerHistoryRepository = customerHistoryRepository;
        this.encryptionService = encryptionService;
        this.customerMapper = customerMapper;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest customerRequest, User authenticatedUser) {
        log.info("Creating customer: firstName={}, lastName={}", customerRequest.getFirstName(), customerRequest.getLastName());

        // Map request to entity
        Customer customer = customerMapper.toCustomerEntity(customerRequest);

        // Handle SSN: hash + encrypt + mask (business logic, not mapping)
        if (customerRequest.getSsn() != null) {
            String ssnHash = encryptionService.hmacSha256(customerRequest.getSsn());

            // Check for duplicate SSN
            Optional<Customer> existingCustomer = customerRepository.findBySsnHash(ssnHash);
            if (existingCustomer.isPresent()) {
                log.warn("Attempt to create customer with duplicate SSN");
                throw new DuplicateSsnException("A customer with this SSN already exists");
            }

            EncryptionResult encryptionResult = encryptionService.encrypt(customerRequest.getSsn().getBytes());
            customer = customerMapper.withEncryptedSsn(customer, encryptionResult, ssnHash, customerRequest);
        }

        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully: id={}", saved.getId());

        // Save addresses using mapper
        List<Address> savedAddresses = List.of();
        if (customerRequest.getAddresses() != null) {
            savedAddresses = customerMapper.toAddressEntities(customerRequest.getAddresses(), saved.getId())
                .stream()
                .map(addressService::save)
                .toList();
        }

        // Write initial history using mapper
        CustomerHistory history = customerMapper.toCustomerHistory(saved, "CREATED");
        history.setChangedBy(authenticatedUser.getUsername());
        customerHistoryRepository.save(history);

        return customerMapper.toCustomerResponse(saved, savedAddresses);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        log.debug("Fetching customer: id={}", id);
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Customer not found: id={}", id);
                return new CustomerNotFoundException("Customer not found with ID: " + id);
            });

        List<Address> addresses = addressService.findAllByCustomerId(customer.getId());
        log.debug("Customer retrieved: id={}, addressCount={}", id, addresses.size());

        return customerMapper.toCustomerResponse(customer, addresses);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerUpdateRequest req, User authenticatedUser) {
        log.info("Updating customer: id={}", id);
        Customer existing = customerRepository.findById(id)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));

        // Create history row of previous state using mapper
        CustomerHistory history = customerMapper.toCustomerHistory(existing, "UPDATED");
        history.setChangedBy(authenticatedUser.getUsername());
        customerHistoryRepository.save(history);

        // Apply updates using mapper
        customerMapper.updateCustomerFromRequest(existing, req);

        // Handle SSN encryption if SSN is being updated (business logic)
        if (req.getSsn() != null) {
            String ssnHash = encryptionService.hmacSha256(req.getSsn());
            EncryptionResult encryptionResult = encryptionService.encrypt(req.getSsn().getBytes());
            existing = customerMapper.withEncryptedSsn(existing, encryptionResult, ssnHash, req);
        }

        Customer saved = customerRepository.save(existing);

        // Handle addresses: delete old and save new (naive approach acceptable for assessment)
        List<Address> oldAddresses = addressService.findAllByCustomerId(saved.getId());
        oldAddresses.forEach(address -> addressService.deleteAddressById(address.getId()));

        List<Address> savedAddresses = List.of();
        if (req.getAddresses() != null) {
            savedAddresses = customerMapper.toAddressEntities(req.getAddresses(), saved.getId())
                .stream()
                .map(addressService::save)
                .toList();
        }

        log.info("Customer updated successfully: id={}", saved.getId());
        return customerMapper.toCustomerResponse(saved, savedAddresses);
    }

    @Transactional(readOnly = true)
    public List<CustomerHistory> getHistory(UUID customerId) {
        log.debug("Fetching history for customer: id={}", customerId);
        List<CustomerHistory> history = customerHistoryRepository.findAllByCustomerIdOrderByChangedAtDesc(customerId);
        log.debug("Retrieved {} history records for customer: id={}", history.size(), customerId);
        return history;
    }

    @Transactional(readOnly = true)
    public CursorPage<CustomerHistory> getHistoryPaginated(UUID customerId, String cursor, Integer limit) {
        // Verify customer exists
        customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        final int pageSize = limit != null ? limit : 20;
        final int fetchSize = pageSize + 1; // Fetch one extra to determine if there's a next page

        log.debug("Fetching paginated history for customer: id={}, cursor={}, pageSize={}",
                  customerId, cursor, pageSize);

        List<CustomerHistory> records;
        if (cursor == null) {
            // First page: fetch the most recent records
            records = customerHistoryRepository.findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
                customerId, OffsetDateTime.now().plusSeconds(1), fetchSize);
        } else {
            // Subsequent pages: fetch after the cursor
            PaginationCursor decodedCursor = PaginationCursor.decode(cursor);
            records = customerHistoryRepository.findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
                customerId, decodedCursor.getTimestamp(), fetchSize);
        }

        // Determine if there's a next page
        boolean hasNextPage = records.size() > pageSize;
        if (hasNextPage) {
            records = records.subList(0, pageSize);
        }

        // Generate next cursor from last item in this page
        String nextCursor = null;
        if (hasNextPage && !records.isEmpty()) {
            CustomerHistory lastItem = records.getLast();
            nextCursor = new PaginationCursor(lastItem.getChangedAt(), lastItem.getId().toString()).encode();
        }

        // Generate previous cursor from first item in this page
        String previousCursor = null;
        if (!records.isEmpty()) {
            CustomerHistory firstItem = records.getFirst();
            // Check if there are records after the first item (indicating we're not at the beginning)
            List<CustomerHistory> afterCheck = customerHistoryRepository
                .findByCustomerIdAndChangedAtGreaterThanOrderByChangedAtAscIdAsc(
                    customerId, firstItem.getChangedAt(), 1);
            if (!afterCheck.isEmpty()) {
                // There's a record after this one, so we can navigate backward
                previousCursor = new PaginationCursor(firstItem.getChangedAt(),
                                                      firstItem.getId().toString()).encode();
            }
        }

        log.debug("Retrieved {} history records for customer: id={}, hasNextPage={}",
                  records.size(), customerId, hasNextPage);

        return new CursorPage<>(records, nextCursor, previousCursor, records.size(), hasNextPage);
    }
}

