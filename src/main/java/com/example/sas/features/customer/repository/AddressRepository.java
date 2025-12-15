package com.example.sas.features.customer.repository;

import com.example.sas.features.customer.entity.Address;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddressRepository extends CrudRepository<Address, UUID> {
    List<Address> findAllByCustomerId(UUID customerId);
}

