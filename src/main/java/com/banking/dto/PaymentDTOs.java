package com.banking.dto;

import com.banking.entity.Payment;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * DTOs for Payment operations.
 */
public class PaymentDTOs {

    public record CreatePaymentRequest(
            @NotBlank(message = "Account number is required")
            String accountNumber,

            @NotNull(message = "Payment type is required")
            Payment.PaymentType paymentType,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            @Digits(integer = 15, fraction = 2)
            BigDecimal amount,

            @NotBlank(message = "Payee name is required")
            @Size(max = 100)
            String payeeName,

            @NotBlank(message = "Payee account number is required")
            String payeeAccountNumber,

            @NotBlank(message = "Payee reference is required")
            String payeeReference,

            LocalDate scheduledDate,

            @Size(max = 500)
            String description
    ) {}

    public record PaymentResponse(
            Long id,
            String paymentReference,
            String accountNumber,
            Payment.PaymentType paymentType,
            BigDecimal amount,
            String payeeName,
            String payeeAccountNumber,
            String payeeReference,
            Payment.PaymentStatus status,
            LocalDate scheduledDate,
            String description,
            Long version,
            Instant createdAt,
            Instant processedAt
    ) {}

    public record PaymentSummary(
            Long id,
            String paymentReference,
            Payment.PaymentType paymentType,
            BigDecimal amount,
            String payeeName,
            Payment.PaymentStatus status,
            Instant createdAt
    ) {}
}
