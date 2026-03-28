package com.banking.api.dto;

import com.banking.api.entity.Transaction;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTOs for Transaction operations.
 */
public class TransactionDTOs {

    public record DepositRequest(
            @NotBlank(message = "Account number is required")
            String accountNumber,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            @Digits(integer = 15, fraction = 2)
            BigDecimal amount,

            @Size(max = 500)
            String description
    ) {}

    public record WithdrawalRequest(
            @NotBlank(message = "Account number is required")
            String accountNumber,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            @Digits(integer = 15, fraction = 2)
            BigDecimal amount,

            @Size(max = 500)
            String description
    ) {}

    public record TransferRequest(
            @NotBlank(message = "From account is required")
            String fromAccountNumber,

            @NotBlank(message = "To account is required")
            String toAccountNumber,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            @Digits(integer = 15, fraction = 2)
            BigDecimal amount,

            @Size(max = 500)
            String description
    ) {}

    public record TransactionResponse(
            Long id,
            String transactionReference,
            String accountNumber,
            String relatedAccountNumber,
            Transaction.TransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Transaction.TransactionStatus status,
            String description,
            String category,
            Long version,
            Instant createdAt
    ) {}

    public record TransactionSummary(
            Long id,
            String transactionReference,
            Transaction.TransactionType type,
            BigDecimal amount,
            Transaction.TransactionStatus status,
            Instant createdAt
    ) {}
}
