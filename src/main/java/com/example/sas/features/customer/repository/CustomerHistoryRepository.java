package com.example.sas.features.customer.repository;

import com.example.sas.features.customer.entity.CustomerHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerHistoryRepository extends CrudRepository<CustomerHistory, UUID> {
    List<CustomerHistory> findAllByCustomerIdOrderByChangedAtDesc(UUID customerId);
}

