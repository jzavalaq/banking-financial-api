package com.banking.api.controller;

import com.banking.api.dto.LoanDTOs.*;
import com.banking.api.dto.PagedResponse;
import com.banking.api.service.LoanService;
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
 * REST Controller for Loan operations.
 *
 * Provides endpoints for loan applications, approvals, disbursements,
 * and loan inquiries.
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loan", description = "Loan management operations")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @Operation(summary = "Apply for a loan", description = "Submits a new loan application")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Loan application submitted"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Customer or account not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> applyForLoan(@Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.applyForLoan(request));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve or reject a loan", description = "Manager action to approve or reject a pending loan")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loan decision recorded"),
        @ApiResponse(responseCode = "400", description = "Loan cannot be approved in current status"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> approveLoan(
            @PathVariable Long id,
            @Valid @RequestBody LoanApprovalRequest request) {
        return ResponseEntity.ok(loanService.approveLoan(id, request));
    }

    @PostMapping("/{id}/disburse")
    @Operation(summary = "Disburse an approved loan", description = "Disburses funds to the customer's account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loan disbursed successfully"),
        @ApiResponse(responseCode = "400", description = "Loan cannot be disbursed in current status"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> disburseLoan(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) LoanDisbursementRequest request) {
        return ResponseEntity.ok(loanService.disburseLoan(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan by ID", description = "Retrieves loan details by internal ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loan found"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @GetMapping("/number/{loanNumber}")
    @Operation(summary = "Get loan by loan number", description = "Retrieves loan details by loan number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loan found"),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LoanResponse> getLoanByNumber(@PathVariable String loanNumber) {
        return ResponseEntity.ok(loanService.getLoanByNumber(loanNumber));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get loans by customer ID", description = "Retrieves paginated list of loans for a customer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Loans retrieved"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PagedResponse<LoanSummary>> getLoansByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(loanService.getLoansByCustomerIdPaged(customerId, page, size));
    }
}
