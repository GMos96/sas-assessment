package com.example.sas.features.customer.service;

import com.example.sas.common.pagination.CursorPage;
import com.example.sas.common.pagination.PaginationCursor;
import com.example.sas.features.customer.entity.CustomerHistory;
import com.example.sas.features.customer.exceptions.CustomerNotFoundException;
import com.example.sas.features.customer.repository.CustomerHistoryRepository;
import com.example.sas.features.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomerService pagination functionality.
 * Uses mocks to test cursor-based pagination logic without requiring Docker/TestContainers.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServicePaginationTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerHistoryRepository customerHistoryRepository;

    private CustomerService customerService;
    private UUID testCustomerId;

    @BeforeEach
    void setUp() {
        // Create a minimal CustomerService with only the dependencies needed for pagination testing
        this.customerService = new CustomerService(customerRepository, null, customerHistoryRepository, null, null);
        this.testCustomerId = UUID.randomUUID();
    }

    @Test
    void getHistoryPaginated_firstPage_returnsLatestRecords() {
        // Given: A customer exists and has history records
        when(customerRepository.findById(testCustomerId))
            .thenReturn(Optional.of(new com.example.sas.features.customer.entity.Customer()));

        List<CustomerHistory> mockHistory = createMockHistory(10);
        when(customerHistoryRepository.findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
            eq(testCustomerId), any(OffsetDateTime.class), eq(11)))
            .thenReturn(mockHistory);

        // When: Fetching the first page without cursor
        CursorPage<CustomerHistory> page = customerService.getHistoryPaginated(testCustomerId, null, 10);

        // Then: We should get records and proper pagination info
        assertThat(page).isNotNull();
        assertThat(page.getItems()).hasSize(10);
        assertThat(page.getPageSize()).isEqualTo(10);
        assertThat(page.isHasNextPage()).isFalse();
    }

    @Test
    void getHistoryPaginated_withLimit_respectsPageSize() {
        // Given: A customer with 25 history records
        when(customerRepository.findById(testCustomerId))
            .thenReturn(Optional.of(new com.example.sas.features.customer.entity.Customer()));

        List<CustomerHistory> mockHistory = createMockHistory(11); // 10 + 1 for hasNextPage check
        when(customerHistoryRepository.findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
            eq(testCustomerId), any(OffsetDateTime.class), eq(11)))
            .thenReturn(mockHistory);

        // When: Fetching with limit=10
        CursorPage<CustomerHistory> page = customerService.getHistoryPaginated(testCustomerId, null, 10);

        // Then: We should get exactly 10 items and a next cursor
        assertThat(page.getItems()).hasSize(10);
        assertThat(page.getPageSize()).isEqualTo(10);
        assertThat(page.isHasNextPage()).isTrue();
        assertThat(page.getNextCursor()).isNotNull();
    }

    @Test
    void getHistoryPaginated_withCursor_decodesCursorCorrectly() {
        // Given: A customer exists and a valid cursor
        when(customerRepository.findById(testCustomerId))
            .thenReturn(Optional.of(new com.example.sas.features.customer.entity.Customer()));

        OffsetDateTime cursorTime = OffsetDateTime.now().minusHours(1);
        PaginationCursor cursor = new PaginationCursor(cursorTime, UUID.randomUUID().toString());
        String encodedCursor = cursor.encode();

        List<CustomerHistory> mockHistory = createMockHistory(5);
        when(customerHistoryRepository.findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
            eq(testCustomerId), any(OffsetDateTime.class), eq(6)))
            .thenReturn(mockHistory);

        // When: Fetching with a cursor
        CursorPage<CustomerHistory> page = customerService.getHistoryPaginated(testCustomerId, encodedCursor, 5);

        // Then: Should fetch records after the cursor timestamp
        assertThat(page.getItems()).hasSize(5);
        assertThat(page.getPageSize()).isEqualTo(5);
    }

    @Test
    void getHistoryPaginated_lastPage_hasNoNextCursor() {
        // Given: A customer with exactly 3 history records
        when(customerRepository.findById(testCustomerId))
            .thenReturn(Optional.of(new com.example.sas.features.customer.entity.Customer()));

        List<CustomerHistory> mockHistory = createMockHistory(3);
        when(customerHistoryRepository.findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
            eq(testCustomerId), any(OffsetDateTime.class), eq(11)))
            .thenReturn(mockHistory);

        // When: Fetch with limit larger than records
        CursorPage<CustomerHistory> page = customerService.getHistoryPaginated(testCustomerId, null, 10);

        // Then: Should have all records and no next cursor
        assertThat(page.getItems()).hasSize(3);
        assertThat(page.isHasNextPage()).isFalse();
        assertThat(page.getNextCursor()).isNull();
    }

    @Test
    void getHistoryPaginated_nonExistentCustomer_throwsException() {
        // Given: A non-existent customer ID
        UUID nonExistentId = UUID.randomUUID();
        when(customerRepository.findById(nonExistentId))
            .thenReturn(Optional.empty());

        // When/Then: Should throw CustomerNotFoundException
        assertThatThrownBy(() -> customerService.getHistoryPaginated(nonExistentId, null, 10))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessageContaining("Customer not found");
    }

    @Test
    void getHistoryPaginated_invalidCursor_throwsException() {
        // Given: A customer and an invalid cursor
        when(customerRepository.findById(testCustomerId))
            .thenReturn(Optional.of(new com.example.sas.features.customer.entity.Customer()));

        // When/Then: Should throw an exception for invalid cursor format
        assertThatThrownBy(() -> customerService.getHistoryPaginated(testCustomerId, "invalid-cursor", 10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getHistoryPaginated_defaultLimit_uses20() {
        // Given: A customer with 30 history records
        when(customerRepository.findById(testCustomerId))
            .thenReturn(Optional.of(new com.example.sas.features.customer.entity.Customer()));

        List<CustomerHistory> mockHistory = createMockHistory(21); // 20 + 1 for hasNextPage check
        when(customerHistoryRepository.findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
            eq(testCustomerId), any(OffsetDateTime.class), eq(21)))
            .thenReturn(mockHistory);

        // When: Fetch without specifying a limit
        CursorPage<CustomerHistory> page = customerService.getHistoryPaginated(testCustomerId, null, null);

        // Then: Should use default limit of 20
        assertThat(page.getItems()).hasSize(20);
        assertThat(page.getPageSize()).isEqualTo(20);
        assertThat(page.isHasNextPage()).isTrue();
    }

    @Test
    void getHistoryPaginated_previousCursor_generatesWhenNotFirstPage() {
        // Given: A customer with history and records after the first item
        when(customerRepository.findById(testCustomerId))
            .thenReturn(Optional.of(new com.example.sas.features.customer.entity.Customer()));

        List<CustomerHistory> mockHistory = createMockHistory(5);
        when(customerHistoryRepository.findByCustomerIdAndChangedAtLessThanOrderByChangedAtDescIdDesc(
            eq(testCustomerId), any(OffsetDateTime.class), eq(6)))
            .thenReturn(mockHistory);

        // Mock that there IS a previous record
        when(customerHistoryRepository.findByCustomerIdAndChangedAtGreaterThanOrderByChangedAtAscIdAsc(
            eq(testCustomerId), any(OffsetDateTime.class), eq(1)))
            .thenReturn(List.of(new CustomerHistory())); // At least one record exists after

        // When: Fetch a page
        CursorPage<CustomerHistory> page = customerService.getHistoryPaginated(testCustomerId, null, 5);

        // Then: Should have a previous cursor
        assertThat(page.getPreviousCursor()).isNotNull();
    }

    private List<CustomerHistory> createMockHistory(int count) {
        List<CustomerHistory> history = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            CustomerHistory h = new CustomerHistory();
            h.setId(UUID.randomUUID());
            h.setCustomerId(testCustomerId);
            h.setFirstName("Test");
            h.setLastName("User");
            h.setChangeType(i == 0 ? "CREATED" : "UPDATED");
            h.setChangedAt(OffsetDateTime.now().minusHours(count - i));
            h.setVersion((long) i);
            history.add(h);
        }
        return history;
    }
}

