package com.example.sas.features.customer.service;

import com.example.sas.features.customer.dto.CustomerRequest;
import com.example.sas.features.customer.dto.CustomerResponse;
import com.example.sas.features.customer.entity.Address;
import com.example.sas.features.customer.entity.Customer;
import com.example.sas.features.customer.entity.CustomerHistory;
import com.example.sas.features.customer.repository.AddressRepository;
import com.example.sas.features.customer.repository.CustomerHistoryRepository;
import com.example.sas.features.customer.repository.CustomerRepository;
import com.example.sas.features.customer.exceptions.CustomerNotFoundException;
import com.example.sas.features.customer.exceptions.DuplicateSsnException;
import com.example.sas.features.customer.mapper.CustomerMapper;
import com.example.sas.common.security.dto.EncryptionResult;
import com.example.sas.common.security.abstractions.EncryptionService;
import com.example.sas.features.customer.util.MaskingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;
    private final CustomerHistoryRepository customerHistoryRepository;
    private final EncryptionService encryptionService;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository,
                           AddressRepository addressRepository,
                           CustomerHistoryRepository customerHistoryRepository,
                           EncryptionService encryptionService,
                           CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
        this.customerHistoryRepository = customerHistoryRepository;
        this.encryptionService = encryptionService;
        this.customerMapper = customerMapper;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest req) {
        log.info("Creating customer: firstName={}, lastName={}", req.getFirstName(), req.getLastName());

        // Map request to entity
        Customer customer = customerMapper.toCustomerEntity(req);

        // Handle SSN: hash + encrypt + mask (business logic, not mapping)
        if (req.getSsn() != null) {
            String ssnHash = encryptionService.hmacSha256(req.getSsn());

            // Check for duplicate SSN
            Optional<Customer> existingCustomer = customerRepository.findBySsnHash(ssnHash);
            if (existingCustomer.isPresent()) {
                log.warn("Attempt to create customer with duplicate SSN");
                throw new DuplicateSsnException("A customer with this SSN already exists");
            }

            EncryptionResult enc = encryptionService.encrypt(req.getSsn().getBytes());
            customer.setSsnHash(ssnHash);
            customer.setSsnEncrypted(enc.getCiphertextBase64());
            customer.setSsnEncryptionKeyId(enc.getKeyId());
            customer.setSsnEncryptedIv(java.util.Base64.getDecoder().decode(enc.getIvBase64()));
            customer.setSsnMasked(MaskingUtil.maskSsn(req.getSsn()));
        }

        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully: id={}", saved.getId());

        // Save addresses using mapper
        List<Address> savedAddresses = List.of();
        if (req.getAddresses() != null) {
            savedAddresses = customerMapper.toAddressEntities(req.getAddresses(), saved.getId())
                .stream()
                .map(addressRepository::save)
                .toList();
        }

        // Write initial history using mapper
        CustomerHistory history = customerMapper.toCustomerHistory(saved, "CREATED");
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

        List<Address> addresses = addressRepository.findAllByCustomerId(customer.getId());
        log.debug("Customer retrieved: id={}, addressCount={}", id, addresses.size());

        return customerMapper.toCustomerResponse(customer, addresses);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerRequest req) {
        log.info("Updating customer: id={}", id);
        Customer existing = customerRepository.findById(id)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));

        // Create history row of previous state using mapper
        CustomerHistory history = customerMapper.toCustomerHistory(existing, "UPDATED");
        customerHistoryRepository.save(history);

        // Apply updates using mapper
        customerMapper.updateCustomerFromRequest(existing, req);

        // Handle SSN encryption if SSN is being updated (business logic)
        if (req.getSsn() != null) {
            String ssnHash = encryptionService.hmacSha256(req.getSsn());
            EncryptionResult enc = encryptionService.encrypt(req.getSsn().getBytes());
            existing.setSsnHash(ssnHash);
            existing.setSsnEncrypted(enc.getCiphertextBase64());
            existing.setSsnEncryptionKeyId(enc.getKeyId());
            existing.setSsnEncryptedIv(java.util.Base64.getDecoder().decode(enc.getIvBase64()));
            existing.setSsnMasked(MaskingUtil.maskSsn(req.getSsn()));
        }

        Customer saved = customerRepository.save(existing);

        // Handle addresses: delete old and save new (naive approach acceptable for assessment)
        List<Address> oldAddresses = addressRepository.findAllByCustomerId(saved.getId());
        oldAddresses.forEach(address -> addressRepository.deleteById(address.getId()));

        List<Address> savedAddresses = List.of();
        if (req.getAddresses() != null) {
            savedAddresses = customerMapper.toAddressEntities(req.getAddresses(), saved.getId())
                .stream()
                .map(addressRepository::save)
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
}

