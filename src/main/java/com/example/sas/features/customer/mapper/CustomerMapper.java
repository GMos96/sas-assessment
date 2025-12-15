package com.example.sas.features.customer.mapper;

import com.example.sas.common.security.dto.EncryptionResult;
import com.example.sas.features.customer.abstractions.UpdatableCustomer;
import com.example.sas.features.customer.dto.AddressRequest;
import com.example.sas.features.customer.dto.AddressResponse;
import com.example.sas.features.customer.dto.CustomerRequest;
import com.example.sas.features.customer.dto.CustomerResponse;
import com.example.sas.features.customer.entity.Address;
import com.example.sas.features.customer.entity.Customer;
import com.example.sas.features.customer.entity.CustomerHistory;
import com.example.sas.features.customer.util.MaskingUtil;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Customer entities and DTOs.
 * Keeps mapping logic separate from business logic for better testability.
 */
@Component
public class CustomerMapper {

    /**
     * Maps Customer entity and associated addresses to CustomerResponse DTO
     */
    public CustomerResponse toCustomerResponse(Customer customer, List<Address> addresses) {
        if (customer == null) {
            return null;
        }

        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setBirthday(customer.getBirthday());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setSsnMasked(customer.getSsnMasked());
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());

        if (addresses != null) {
            List<AddressResponse> addressResponses = addresses.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
            response.setAddresses(addressResponses);
        }

        return response;
    }

    /**
     * Maps CustomerRequest DTO to new Customer entity
     * Note: Does NOT handle SSN encryption - that's business logic handled by service
     */
    public Customer toCustomerEntity(CustomerRequest request) {
        if (request == null) {
            return null;
        }

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setBirthday(request.getBirthday());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setCreatedAt(OffsetDateTime.now());
        customer.setUpdatedAt(OffsetDateTime.now());

        return customer;
    }

    public Customer withEncryptedSsn(Customer customer, EncryptionResult encryptionResult, String ssnHash, CustomerRequest customerRequest) {
        if (customer == null) {
            return null;
        }

        customer.setSsnHash(ssnHash);
        customer.setSsnEncrypted(encryptionResult.getCiphertextBase64());
        customer.setSsnEncryptionKeyId(encryptionResult.getKeyId());
        customer.setSsnEncryptedIv(java.util.Base64.getDecoder().decode(encryptionResult.getIvBase64()));
        customer.setSsnMasked(MaskingUtil.maskSsn(customerRequest.getSsn()));

        return customer;
    }

    /**
     * Updates existing Customer entity with values from CustomerRequest
     * Only updates non-null fields from request
     * Note: Does NOT handle SSN encryption - that's business logic handled by service
     */
    public void updateCustomerFromRequest(Customer customer, UpdatableCustomer request) {
        if (customer == null || request == null) {
            return;
        }

        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getBirthday() != null) {
            customer.setBirthday(request.getBirthday());
        }
        if (request.getEmail() != null) {
            customer.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone());
        }

        customer.setUpdatedAt(OffsetDateTime.now());
    }

    /**
     * Maps AddressRequest DTO to Address entity
     */
    public Address toAddressEntity(AddressRequest request, UUID customerId) {
        if (request == null) {
            return null;
        }

        Address address = new Address();
        address.setCustomerId(customerId);
        address.setType(request.getType());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        return address;
    }

    /**
     * Maps Address entity to AddressResponse DTO
     */
    public AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }

        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setType(address.getType());
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setPostalCode(address.getPostalCode());
        response.setCountry(address.getCountry());

        return response;
    }

    /**
     * Maps list of AddressRequest DTOs to list of Address entities
     */
    public List<Address> toAddressEntities(List<AddressRequest> requests, UUID customerId) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
            .map(request -> toAddressEntity(request, customerId))
            .collect(Collectors.toList());
    }

    /**
     * Creates CustomerHistory from existing Customer entity
     * Used for audit trail purposes
     */
    public CustomerHistory toCustomerHistory(Customer customer, String changeType) {
        if (customer == null) {
            return null;
        }

        CustomerHistory history = new CustomerHistory();
        history.setCustomerId(customer.getId());
        history.setFirstName(customer.getFirstName());
        history.setLastName(customer.getLastName());
        history.setBirthday(customer.getBirthday());
        history.setEmail(customer.getEmail());
        history.setPhone(customer.getPhone());
        history.setSsnEncrypted(customer.getSsnEncrypted());
        history.setSsnEncryptedIv(customer.getSsnEncryptedIv());
        history.setSsnEncryptionKeyId(customer.getSsnEncryptionKeyId());
        history.setSsnHash(customer.getSsnHash());
        history.setSsnMasked(customer.getSsnMasked());
        history.setChangeType(changeType);
        history.setChangedAt(OffsetDateTime.now());
        history.setVersion(customer.getVersion());

        return history;
    }
}

