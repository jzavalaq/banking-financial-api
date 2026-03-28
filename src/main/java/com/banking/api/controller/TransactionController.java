package com.banking.api.controller;

import com.banking.api.dto.PagedResponse;
import com.banking.api.dto.TransactionDTOs.*;
import com.banking.api.service.TransactionService;
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
 * REST Controller for Transaction operations.
 *
 * Provides endpoints for financial transactions including deposits,
 * withdrawals, and transfers between accounts.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "Transaction operations")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit to account", description = "Deposits funds into the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Deposit successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request or inactive account"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.deposit(request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw from account", description = "Withdraws funds from the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Withdrawal successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request or inactive account"),
        @ApiResponse(responseCode = "422", description = "Insufficient funds"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.withdraw(request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer between accounts", description = "Transfers funds between two accounts")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transfer successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request or inactive accounts"),
        @ApiResponse(responseCode = "422", description = "Insufficient funds"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(request));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get transaction by reference", description = "Retrieves transaction details by reference number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction found"),
        @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String reference) {
        return ResponseEntity.ok(transactionService.getTransactionByReference(reference));
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get transaction history for account", description = "Retrieves paginated transaction history")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction history retrieved"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PagedResponse<TransactionSummary>> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber, page, size));
    }
}
