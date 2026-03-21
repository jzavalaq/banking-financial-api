package com.banking.service;

import com.banking.dto.AccountDTOs.*;
import com.banking.dto.PagedResponse;
import com.banking.entity.Account;
import com.banking.entity.Customer;
import com.banking.exception.BadRequestException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for Account operations.
 *
 * Handles business logic for account creation, retrieval, balance inquiries,
 * and status management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal DEFAULT_CHECKING_INTEREST = new BigDecimal("0.001");
    private static final BigDecimal DEFAULT_SAVINGS_INTEREST = new BigDecimal("0.025");

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for customer ID: {}", request.customerId());

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.customerId()));

        String accountNumber = generateAccountNumber();

        BigDecimal interestRate = request.accountType() == Account.AccountType.CHECKING
                ? DEFAULT_CHECKING_INTEREST
                : DEFAULT_SAVINGS_INTEREST;

        BigDecimal overdraftLimit = request.overdraftLimit() != null
                ? request.overdraftLimit()
                : BigDecimal.ZERO;

        BigDecimal initialDeposit = request.initialDeposit() != null
                ? request.initialDeposit()
                : BigDecimal.ZERO;

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customer(customer)
                .accountType(request.accountType())
                .balance(initialDeposit)
                .status(Account.AccountStatus.ACTIVE)
                .interestRate(interestRate)
                .overdraftLimit(overdraftLimit)
                .build();

        account = accountRepository.save(account);
        log.info("Created account {} for customer {}", accountNumber, customer.getId());

        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        return new BalanceResponse(
                account.getAccountNumber(),
                account.getBalance().add(account.getOverdraftLimit()),
                account.getBalance(),
                account.getOverdraftLimit(),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<AccountSummary> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountSummary> getAccountsByCustomerIdPaged(Long customerId, int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());
        Page<Account> accountPage = accountRepository.findByCustomerId(customerId, pageable);

        return new PagedResponse<>(
                accountPage.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                accountPage.getNumber(),
                accountPage.getSize(),
                accountPage.getTotalElements(),
                accountPage.getTotalPages(),
                accountPage.isFirst(),
                accountPage.isLast()
        );
    }

    @Transactional
    public AccountResponse updateAccountStatus(Long id, UpdateAccountStatusRequest request) {
        log.info("Updating account {} status to {}", id, request.status());

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        // Business rules for status changes
        if (account.getStatus() == Account.AccountStatus.CLOSED) {
            throw new BadRequestException("Cannot modify a closed account");
        }

        if (request.status() == Account.AccountStatus.CLOSED) {
            if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("Cannot close account with non-zero balance");
            }
        }

        account.setStatus(request.status());
        account = accountRepository.save(account);

        log.info("Account {} status updated to {}", account.getAccountNumber(), request.status());
        return toResponse(account);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "BA" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomer().getId(),
                account.getCustomer().getFullName(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getInterestRate(),
                account.getOverdraftLimit(),
                account.getVersion(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    private AccountSummary toSummary(Account account) {
        return new AccountSummary(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus()
        );
    }
}
