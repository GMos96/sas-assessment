package com.example.sas.features.customer.repository;

import com.example.sas.features.customer.entity.CustomerHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerHistoryRepository extends CrudRepository<CustomerHistory, UUID> {
    List<CustomerHistory> findAllByCustomerIdOrderByChangedAtDesc(UUID customerId);

    /**
     * Fetch history records after a given timestamp in descending order.
     * Used for cursor-based pagination (forward direction).
     *
     * @param customerId the customer ID
     * @param afterTimestamp the cursor timestamp (exclusive)
     * @param limit the maximum number of records to fetch
     * @return list of history records
     */
    List<CustomerHistory> findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
        UUID customerId, OffsetDateTime afterTimestamp, int limit);

    /**
     * Fetch history records before a given timestamp in ascending order.
     * Used for cursor-based pagination (backward direction).
     *
     * @param customerId the customer ID
     * @param beforeTimestamp the cursor timestamp (exclusive)
     * @param limit the maximum number of records to fetch
     * @return list of history records
     */
    List<CustomerHistory> findByCustomerIdAndChangedAtGreaterThanOrderByChangedAtAscIdAsc(
        UUID customerId, OffsetDateTime beforeTimestamp, int limit);
}

