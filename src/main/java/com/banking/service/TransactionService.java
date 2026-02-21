package com.banking.service;

import com.banking.dto.PagedResponse;
import com.banking.dto.TransactionDTOs.*;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.exception.BadRequestException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
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
import java.util.UUID;

/**
 * Service for Transaction operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        log.info("Processing deposit of {} to account {}", request.amount(), request.accountNumber());

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.accountNumber()));

        if (!account.isActive()) {
            throw new BadRequestException("Cannot deposit to inactive account");
        }

        String reference = generateTransactionReference();

        Transaction transaction = Transaction.builder()
                .transactionReference(reference)
                .account(account)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(request.amount())
                .balanceAfter(account.getBalance().add(request.amount()))
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.description())
                .category("DEPOSIT")
                .build();

        // Update account balance
        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);

        transaction = transactionRepository.save(transaction);
        log.info("Deposit completed with reference: {}", reference);

        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawalRequest request) {
        log.info("Processing withdrawal of {} from account {}", request.amount(), request.accountNumber());

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.accountNumber()));

        if (!account.isActive()) {
            throw new BadRequestException("Cannot withdraw from inactive account");
        }

        if (!account.hasSufficientFunds(request.amount())) {
            throw new InsufficientFundsException(
                    account.getAccountNumber(),
                    request.amount().toString(),
                    account.getBalance().toString()
            );
        }

        String reference = generateTransactionReference();

        Transaction transaction = Transaction.builder()
                .transactionReference(reference)
                .account(account)
                .type(Transaction.TransactionType.WITHDRAWAL)
                .amount(request.amount())
                .balanceAfter(account.getBalance().subtract(request.amount()))
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.description())
                .category("WITHDRAWAL")
                .build();

        // Update account balance
        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);

        transaction = transactionRepository.save(transaction);
        log.info("Withdrawal completed with reference: {}", reference);

        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        log.info("Processing transfer of {} from {} to {}",
                request.amount(), request.fromAccountNumber(), request.toAccountNumber());

        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }

        Account fromAccount = accountRepository.findByAccountNumber(request.fromAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.fromAccountNumber()));

        Account toAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.toAccountNumber()));

        if (!fromAccount.isActive()) {
            throw new BadRequestException("Source account is not active");
        }
        if (!toAccount.isActive()) {
            throw new BadRequestException("Destination account is not active");
        }

        if (!fromAccount.hasSufficientFunds(request.amount())) {
            throw new InsufficientFundsException(
                    fromAccount.getAccountNumber(),
                    request.amount().toString(),
                    fromAccount.getBalance().toString()
            );
        }

        String reference = generateTransactionReference();

        // Create transfer out transaction
        Transaction transferOut = Transaction.builder()
                .transactionReference(reference + "-OUT")
                .account(fromAccount)
                .relatedAccount(toAccount)
                .type(Transaction.TransactionType.TRANSFER_OUT)
                .amount(request.amount())
                .balanceAfter(fromAccount.getBalance().subtract(request.amount()))
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.description())
                .category("TRANSFER")
                .build();

        // Create transfer in transaction
        Transaction transferIn = Transaction.builder()
                .transactionReference(reference + "-IN")
                .account(toAccount)
                .relatedAccount(fromAccount)
                .type(Transaction.TransactionType.TRANSFER_IN)
                .amount(request.amount())
                .balanceAfter(toAccount.getBalance().add(request.amount()))
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.description())
                .category("TRANSFER")
                .build();

        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transactionRepository.save(transferOut);
        transactionRepository.save(transferIn);

        log.info("Transfer completed with reference: {}", reference);

        return toResponse(transferOut);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionSummary> getTransactionHistory(String accountNumber, int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());
        Page<Transaction> transactionPage = transactionRepository.findByAccountId(account.getId(), pageable);

        return new PagedResponse<>(
                transactionPage.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isFirst(),
                transactionPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String reference) {
        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "reference", reference));
        return toResponse(transaction);
    }

    private String generateTransactionReference() {
        return "TXN" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionReference(),
                transaction.getAccount().getAccountNumber(),
                transaction.getRelatedAccount() != null
                        ? transaction.getRelatedAccount().getAccountNumber()
                        : null,
                transaction.getType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCategory(),
                transaction.getCreatedAt()
        );
    }

    private TransactionSummary toSummary(Transaction transaction) {
        return new TransactionSummary(
                transaction.getId(),
                transaction.getTransactionReference(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
