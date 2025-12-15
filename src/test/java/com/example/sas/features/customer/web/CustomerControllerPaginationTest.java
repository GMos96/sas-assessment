package com.example.sas.features.customer.web;

import com.example.sas.common.pagination.CursorPage;
import com.example.sas.core.config.errorhandling.GlobalExceptionHandler;
import com.example.sas.features.customer.entity.CustomerHistory;
import com.example.sas.features.customer.exceptions.CustomerNotFoundException;
import com.example.sas.features.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CustomerController pagination endpoints.
 * Tests HTTP responses and request validation without TestContainers.
 */
@ExtendWith(MockitoExtension.class)
@WithMockUser(username = "admin", roles = "USER")
class CustomerControllerPaginationTest {

    @Mock
    private CustomerService customerService;

    private MockMvc mockMvc;
    private UUID testCustomerId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CustomerController(customerService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        testCustomerId = UUID.randomUUID();
    }

    @Test
    void getHistoryPaginated_returnsValidCursorPageResponse() throws Exception {
        // Given: A mock customer history page
        CursorPage<CustomerHistory> mockPage = createMockPage(5, true);
        when(customerService.getHistoryPaginated(eq(testCustomerId), isNull(), eq(10)))
            .thenReturn(mockPage);

        // When: Request paginated history
        mockMvc.perform(
            get("/api/customers/{id}/history", testCustomerId)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(5))
            .andExpect(jsonPath("$.pageSize").value(5))
            .andExpect(jsonPath("$.hasNextPage").value(true))
            .andExpect(jsonPath("$.nextCursor").exists())
            .andExpect(jsonPath("$.previousCursor").doesNotExist());
    }

    @Test
    void getHistoryPaginated_withInvalidLimit_returnsBadRequest() throws Exception {
        // When: Request with limit > 100
        mockMvc.perform(
            get("/api/customers/{id}/history", testCustomerId)
                .param("limit", "200"))
            .andExpect(status().isBadRequest());

        // When: Request with limit < 1
        mockMvc.perform(
            get("/api/customers/{id}/history", testCustomerId)
                .param("limit", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getHistoryPaginated_withValidCursor_returnsPaginatedResponse() throws Exception {
        // Given: A valid cursor and mock page
        String testCursor = "dGVzdGN1cnNvcg==";
        CursorPage<CustomerHistory> mockPage = createMockPage(5, true);
        when(customerService.getHistoryPaginated(eq(testCustomerId), eq(testCursor), eq(10)))
            .thenReturn(mockPage);

        // When: Request with cursor
        mockMvc.perform(
            get("/api/customers/{id}/history", testCustomerId)
                .param("cursor", testCursor)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.pageSize").value(5));
    }

    @Test
    void getHistoryPaginated_nonExistentCustomer_returns404() throws Exception {
        // Given: Customer not found exception
        UUID nonExistentId = UUID.randomUUID();
        when(customerService.getHistoryPaginated(eq(nonExistentId), any(), any()))
            .thenThrow(new CustomerNotFoundException("Customer not found with ID: " + nonExistentId));

        // When: Request for non-existent customer
        mockMvc.perform(
            get("/api/customers/{id}/history", nonExistentId))
            .andExpect(status().isNotFound());
    }

    @Test
    void getHistoryPaginated_lastPage_hasNoNextCursor() throws Exception {
        // Given: Last page with no next cursor
        CursorPage<CustomerHistory> mockPage = createMockPage(3, false);
        when(customerService.getHistoryPaginated(eq(testCustomerId), isNull(), any()))
            .thenReturn(mockPage);

        // When: Request last page
        mockMvc.perform(
            get("/api/customers/{id}/history", testCustomerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasNextPage").value(false))
            .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void getHistoryPaginated_returnsMostRecentFirst() throws Exception {
        // Given: History ordered by most recent first
        CursorPage<CustomerHistory> mockPage = new CursorPage<>();
        List<CustomerHistory> items = new java.util.ArrayList<>();

        CustomerHistory h1 = new CustomerHistory();
        h1.setId(UUID.randomUUID());
        h1.setChangeType("UPDATED");
        h1.setChangedAt(OffsetDateTime.now());
        items.add(h1);

        CustomerHistory h2 = new CustomerHistory();
        h2.setId(UUID.randomUUID());
        h2.setChangeType("CREATED");
        h2.setChangedAt(OffsetDateTime.now().minusHours(1));
        items.add(h2);

        mockPage.setItems(items);
        mockPage.setPageSize(2);
        mockPage.setHasNextPage(false);

        when(customerService.getHistoryPaginated(eq(testCustomerId), any(), any()))
            .thenReturn(mockPage);

        // When: Request history
        mockMvc.perform(
            get("/api/customers/{id}/history", testCustomerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].changeType").value("UPDATED"))
            .andExpect(jsonPath("$.items[1].changeType").value("CREATED"));
    }

    private CursorPage<CustomerHistory> createMockPage(int itemCount, boolean hasNext) {
        CursorPage<CustomerHistory> page = new CursorPage<>();

        List<CustomerHistory> items = new java.util.ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            CustomerHistory h = new CustomerHistory();
            h.setId(UUID.randomUUID());
            h.setCustomerId(testCustomerId);
            h.setFirstName("Test");
            h.setLastName("User");
            h.setChangeType(i == 0 ? "CREATED" : "UPDATED");
            h.setChangedAt(OffsetDateTime.now().minusHours(itemCount - i));
            items.add(h);
        }

        page.setItems(items);
        page.setPageSize(itemCount);
        page.setHasNextPage(hasNext);
        if (hasNext) {
            page.setNextCursor("bmV4dGN1cnNvcg==");
        }

        return page;
    }
}

