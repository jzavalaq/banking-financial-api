package com.banking.dto;

import com.banking.entity.Loan;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * DTOs for Loan operations.
 */
public class LoanDTOs {

    public record LoanApplicationRequest(
            @NotNull(message = "Customer ID is required")
            Long customerId,

            @NotBlank(message = "Disbursement account is required")
            String disbursementAccountNumber,

            @NotNull(message = "Loan type is required")
            Loan.LoanType loanType,

            @NotNull(message = "Principal amount is required")
            @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1,000")
            @Digits(integer = 15, fraction = 2)
            BigDecimal principalAmount,

            @NotNull(message = "Term in months is required")
            @Min(value = 6, message = "Minimum term is 6 months")
            @Max(value = 360, message = "Maximum term is 360 months")
            Integer termMonths,

            @Size(max = 500)
            String purpose
    ) {}

    public record LoanApprovalRequest(
            @NotNull(message = "Approved is required")
            Boolean approved,

            @DecimalMin(value = "0.01", message = "Interest rate must be positive")
            BigDecimal approvedInterestRate,

            String rejectionReason
    ) {}

    public record LoanDisbursementRequest(
            @Size(max = 500)
            String notes
    ) {}

    public record LoanResponse(
            Long id,
            String loanNumber,
            Long customerId,
            String customerName,
            String disbursementAccountNumber,
            Loan.LoanType loanType,
            BigDecimal principalAmount,
            BigDecimal interestRate,
            Integer termMonths,
            BigDecimal outstandingBalance,
            BigDecimal monthlyPayment,
            Loan.LoanStatus status,
            LocalDate applicationDate,
            LocalDate approvalDate,
            LocalDate disbursementDate,
            LocalDate nextPaymentDate,
            String purpose,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record LoanSummary(
            Long id,
            String loanNumber,
            Loan.LoanType loanType,
            BigDecimal principalAmount,
            BigDecimal outstandingBalance,
            Loan.LoanStatus status
    ) {}
}
