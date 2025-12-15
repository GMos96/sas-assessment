package com.example.sas.features.customer.repository;

import com.example.sas.features.customer.entity.Customer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends CrudRepository<Customer, UUID> {
    Optional<Customer> findBySsnHash(String ssnHash);
}

