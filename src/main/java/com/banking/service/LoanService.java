package com.banking.service;

import com.banking.dto.LoanDTOs.*;
import com.banking.dto.PagedResponse;
import com.banking.entity.Account;
import com.banking.entity.Customer;
import com.banking.entity.Loan;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.CustomerRepository;
import com.banking.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for Loan operations.
 *
 * Handles business logic for loan applications, approvals, disbursements,
 * and loan inquiries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional
    public LoanResponse applyForLoan(LoanApplicationRequest request) {
        log.info("Processing loan application for customer {}", request.customerId());

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.customerId()));

        Account disbursementAccount = accountRepository.findByAccountNumber(request.disbursementAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.disbursementAccountNumber()));

        if (!disbursementAccount.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Disbursement account must belong to the customer");
        }

        if (!disbursementAccount.isActive()) {
            throw new BadRequestException("Disbursement account must be active");
        }

        String loanNumber = generateLoanNumber();
        BigDecimal monthlyPayment = calculateMonthlyPayment(
                request.principalAmount(),
                request.loanType() == Loan.LoanType.MORTGAGE ? new BigDecimal("0.065") : new BigDecimal("0.085"),
                request.termMonths()
        );

        Loan loan = Loan.builder()
                .loanNumber(loanNumber)
                .customer(customer)
                .disbursementAccount(disbursementAccount)
                .loanType(request.loanType())
                .principalAmount(request.principalAmount())
                .interestRate(request.loanType() == Loan.LoanType.MORTGAGE
                        ? new BigDecimal("0.065")
                        : new BigDecimal("0.085"))
                .termMonths(request.termMonths())
                .outstandingBalance(BigDecimal.ZERO)
                .monthlyPayment(monthlyPayment)
                .status(Loan.LoanStatus.PENDING)
                .applicationDate(LocalDate.now())
                .purpose(request.purpose())
                .build();

        loan = loanRepository.save(loan);
        log.info("Loan application created with number: {}", loanNumber);

        return toResponse(loan);
    }

    @Transactional
    public LoanResponse approveLoan(Long id, LoanApprovalRequest request) {
        log.info("Processing loan approval for loan {}", id);

        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", id));

        if (!loan.isApprovable()) {
            throw new BadRequestException("Loan cannot be approved in status: " + loan.getStatus());
        }

        if (request.approved()) {
            BigDecimal interestRate = request.approvedInterestRate() != null
                    ? request.approvedInterestRate()
                    : loan.getInterestRate();

            BigDecimal monthlyPayment = calculateMonthlyPayment(
                    loan.getPrincipalAmount(),
                    interestRate,
                    loan.getTermMonths()
            );

            loan.setInterestRate(interestRate);
            loan.setMonthlyPayment(monthlyPayment);
            loan.setStatus(Loan.LoanStatus.APPROVED);
            loan.setApprovalDate(LocalDate.now());
        } else {
            loan.setStatus(Loan.LoanStatus.REJECTED);
        }

        loan = loanRepository.save(loan);
        log.info("Loan {} {}", loan.getLoanNumber(), request.approved() ? "approved" : "rejected");

        return toResponse(loan);
    }

    @Transactional
    public LoanResponse disburseLoan(Long id, LoanDisbursementRequest request) {
        log.info("Disbursing loan {}", id);

        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", id));

        if (!loan.isDisbursable()) {
            throw new BadRequestException("Loan cannot be disbursed in status: " + loan.getStatus());
        }

        Account account = loan.getDisbursementAccount();

        // Credit the disbursement account
        account.setBalance(account.getBalance().add(loan.getPrincipalAmount()));
        accountRepository.save(account);

        // Update loan status
        loan.setOutstandingBalance(loan.getPrincipalAmount());
        loan.setStatus(Loan.LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setNextPaymentDate(LocalDate.now().plusMonths(1));

        loan = loanRepository.save(loan);
        log.info("Loan {} disbursed to account {}", loan.getLoanNumber(), account.getAccountNumber());

        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", id));
        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanByNumber(String loanNumber) {
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));
        return toResponse(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanSummary> getLoansByCustomerId(Long customerId) {
        return loanRepository.findByCustomerId(customerId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<LoanSummary> getLoansByCustomerIdPaged(Long customerId, int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());

        Page<Loan> loanPage = loanRepository.findByCustomerId(customerId, pageable);

        return new PagedResponse<>(
                loanPage.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                loanPage.getNumber(),
                loanPage.getSize(),
                loanPage.getTotalElements(),
                loanPage.getTotalPages(),
                loanPage.isFirst(),
                loanPage.isLast()
        );
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        double r = monthlyRate.doubleValue();
        double p = principal.doubleValue();
        int n = termMonths;

        double payment = p * (r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);

        return BigDecimal.valueOf(payment).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateLoanNumber() {
        String loanNumber;
        do {
            loanNumber = "LN" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } while (loanRepository.existsByLoanNumber(loanNumber));
        return loanNumber;
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getLoanNumber(),
                loan.getCustomer().getId(),
                loan.getCustomer().getFullName(),
                loan.getDisbursementAccount() != null
                        ? loan.getDisbursementAccount().getAccountNumber()
                        : null,
                loan.getLoanType(),
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTermMonths(),
                loan.getOutstandingBalance(),
                loan.getMonthlyPayment(),
                loan.getStatus(),
                loan.getApplicationDate(),
                loan.getApprovalDate(),
                loan.getDisbursementDate(),
                loan.getNextPaymentDate(),
                loan.getPurpose(),
                loan.getVersion(),
                loan.getCreatedAt(),
                loan.getUpdatedAt()
        );
    }

    private LoanSummary toSummary(Loan loan) {
        return new LoanSummary(
                loan.getId(),
                loan.getLoanNumber(),
                loan.getLoanType(),
                loan.getPrincipalAmount(),
                loan.getOutstandingBalance(),
                loan.getStatus()
        );
    }
}
