package com.banking.controller;

import com.banking.dto.LoanDTOs.*;
import com.banking.dto.PagedResponse;
import com.banking.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Loan operations.
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan", description = "Loan management operations")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @Operation(summary = "Apply for a loan")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> applyForLoan(@Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.applyForLoan(request));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve or reject a loan")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> approveLoan(
            @PathVariable Long id,
            @Valid @RequestBody LoanApprovalRequest request) {
        return ResponseEntity.ok(loanService.approveLoan(id, request));
    }

    @PostMapping("/{id}/disburse")
    @Operation(summary = "Disburse an approved loan")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> disburseLoan(
            @PathVariable Long id,
            @RequestBody(required = false) LoanDisbursementRequest request) {
        return ResponseEntity.ok(loanService.disburseLoan(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan by ID")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @GetMapping("/number/{loanNumber}")
    @Operation(summary = "Get loan by loan number")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> getLoanByNumber(@PathVariable String loanNumber) {
        return ResponseEntity.ok(loanService.getLoanByNumber(loanNumber));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get loans by customer ID")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<LoanSummary>> getLoansByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(loanService.getLoansByCustomerId(customerId));
    }
}
