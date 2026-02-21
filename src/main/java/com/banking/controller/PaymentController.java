package com.banking.controller;

import com.banking.dto.PagedResponse;
import com.banking.dto.PaymentDTOs.*;
import com.banking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Payment operations.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment operations")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create a payment")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get payment by reference")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> getPaymentByReference(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.getPaymentByReference(reference));
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get payments by account")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PagedResponse<PaymentSummary>> getPaymentsByAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(paymentService.getPaymentsByAccount(accountNumber, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel payment")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.cancelPayment(id));
    }
}
