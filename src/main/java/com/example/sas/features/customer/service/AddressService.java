package com.example.sas.features.customer.service;

import com.example.sas.features.customer.entity.Address;
import com.example.sas.features.customer.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<Address> findAllByCustomerId(UUID customerId) {
        return addressRepository.findAllByCustomerId(customerId);
    }

    public Address save(Address address) {
        return addressRepository.save(address);
    }

    public void deleteAddressById(UUID address) {
        addressRepository.deleteById(address);
    }
}
