package com.banking.api.dto;

import com.banking.api.entity.Account;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTOs for Account operations.
 */
public class AccountDTOs {

    public record CreateAccountRequest(
            @NotNull(message = "Customer ID is required")
            Long customerId,

            @NotNull(message = "Account type is required")
            Account.AccountType accountType,

            @DecimalMin(value = "0.0", inclusive = false, message = "Initial deposit must be positive")
            BigDecimal initialDeposit,

            @DecimalMin(value = "0.0", message = "Overdraft limit cannot be negative")
            BigDecimal overdraftLimit
    ) {}

    public record UpdateAccountStatusRequest(
            @NotNull(message = "Status is required")
            Account.AccountStatus status,

            String reason
    ) {}

    public record AccountResponse(
            Long id,
            String accountNumber,
            Long customerId,
            String customerName,
            Account.AccountType accountType,
            BigDecimal balance,
            Account.AccountStatus status,
            BigDecimal interestRate,
            BigDecimal overdraftLimit,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record AccountSummary(
            Long id,
            String accountNumber,
            Account.AccountType accountType,
            BigDecimal balance,
            Account.AccountStatus status
    ) {}

    public record BalanceResponse(
            String accountNumber,
            BigDecimal availableBalance,
            BigDecimal currentBalance,
            BigDecimal overdraftLimit,
            Instant asOf
    ) {}
}
