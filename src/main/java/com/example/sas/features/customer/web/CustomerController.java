package com.example.sas.features.customer.web;

import com.example.sas.features.customer.dto.CustomerRequest;
import com.example.sas.features.customer.dto.CustomerResponse;
import com.example.sas.features.customer.dto.CustomerUpdateRequest;
import com.example.sas.features.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer Management", description = "APIs for managing customer records with encrypted SSN and full audit history")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = "Create a new customer", description = "Creates a new customer record with encrypted SSN and multiple addresses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest req, @AuthenticationPrincipal User user) {
        CustomerResponse resp = customerService.createCustomer(req, user);
        return ResponseEntity.created(URI.create("/api/customers/" + resp.getId())).body(resp);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves a customer record by their unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer found",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable("id") UUID id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Updates an existing customer record and stores the change in history")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer updated successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable("id") UUID id,
            @Valid @RequestBody CustomerUpdateRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(customerService.updateCustomer(id, req, user));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get customer history", description = "Retrieves the audit history of all changes made to a customer record. Supports cursor-based pagination with optional cursor and limit parameters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Object.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid limit parameter", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<?> getHistory(
            @Parameter(description = "Customer ID", required = true) @PathVariable("id") UUID id,
            @Parameter(description = "Cursor for pagination (obtained from nextCursor in previous response). Omit for first page.")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "Number of records per page (default: 20, max: 100). Required", example = "20")
            @RequestParam(value = "limit", required = false) Integer limit) {

        // Validate limit parameter
        if (limit != null && (limit < 1 || limit > 100)) {
            return ResponseEntity.badRequest().build();
        }

        // Delegate to service layer - service handles pagination logic
        return ResponseEntity.ok(customerService.getHistoryPaginated(id, cursor, limit));
    }
}

