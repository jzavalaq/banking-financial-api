package com.banking.api.controller;

import com.banking.api.dto.CustomerDTOs.*;
import com.banking.api.dto.PagedResponse;
import com.banking.api.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Customer operations.
 *
 * Provides endpoints for customer management including creation,
 * retrieval, updates, and search functionality.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer management operations")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a new customer", description = "Creates a new customer with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Customer created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Customer with email or tax ID already exists")
    })
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer details by internal ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer found"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Updates customer information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @GetMapping
    @Operation(summary = "List all customers with pagination", description = "Retrieves paginated list of customers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    })
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PagedResponse<CustomerSummary>> listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(customerService.listCustomers(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers", description = "Search customers by name, email, or phone")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved")
    })
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PagedResponse<CustomerSummary>> searchCustomers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(customerService.searchCustomers(q, page, size));
    }
}
