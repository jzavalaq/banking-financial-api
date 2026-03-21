package com.banking.service;

import com.banking.dto.PagedResponse;
import com.banking.dto.PaymentDTOs.*;
import com.banking.entity.Account;
import com.banking.entity.Payment;
import com.banking.exception.BadRequestException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for Payment operations.
 *
 * Handles business logic for creating, processing, and managing
 * bill payments and transfers to external payees.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment of {} from account {}", request.amount(), request.accountNumber());

        Account account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", request.accountNumber()));

        if (!account.isActive()) {
            throw new BadRequestException("Cannot create payment from inactive account");
        }

        if (!account.hasSufficientFunds(request.amount())) {
            throw new InsufficientFundsException(
                    account.getAccountNumber(),
                    request.amount().toString(),
                    account.getBalance().toString()
            );
        }

        String paymentReference = generatePaymentReference();

        Payment payment = Payment.builder()
                .paymentReference(paymentReference)
                .account(account)
                .paymentType(request.paymentType())
                .amount(request.amount())
                .payeeName(request.payeeName())
                .payeeAccountNumber(request.payeeAccountNumber())
                .payeeReference(request.payeeReference())
                .status(Payment.PaymentStatus.PENDING)
                .scheduledDate(request.scheduledDate())
                .description(request.description())
                .build();

        // Process immediately if no scheduled date or scheduled for today
        if (request.scheduledDate() == null || !request.scheduledDate().isAfter(java.time.LocalDate.now())) {
            processPayment(payment, account);
        }

        payment = paymentRepository.save(payment);
        log.info("Payment created with reference: {}", paymentReference);

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(Long id) {
        log.info("Cancelling payment with ID: {}", id);

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        if (!payment.isCancellable()) {
            throw new BadRequestException("Payment cannot be cancelled in status: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment.setProcessedAt(Instant.now());
        payment = paymentRepository.save(payment);

        log.info("Payment {} cancelled", payment.getPaymentReference());
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String reference) {
        Payment payment = paymentRepository.findByPaymentReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "reference", reference));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentSummary> getPaymentsByAccount(String accountNumber, int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));

        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());
        Page<Payment> paymentPage = paymentRepository.findByAccountId(account.getId(), pageable);

        return new PagedResponse<>(
                paymentPage.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements(),
                paymentPage.getTotalPages(),
                paymentPage.isFirst(),
                paymentPage.isLast()
        );
    }

    private void processPayment(Payment payment, Account account) {
        // Deduct from account
        account.setBalance(account.getBalance().subtract(payment.getAmount()));
        accountRepository.save(account);

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setProcessedAt(Instant.now());
    }

    private String generatePaymentReference() {
        return "PAY" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getAccount().getAccountNumber(),
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getPayeeName(),
                payment.getPayeeAccountNumber(),
                payment.getPayeeReference(),
                payment.getStatus(),
                payment.getScheduledDate(),
                payment.getDescription(),
                payment.getVersion(),
                payment.getCreatedAt(),
                payment.getProcessedAt()
        );
    }

    private PaymentSummary toSummary(Payment payment) {
        return new PaymentSummary(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getPaymentType(),
                payment.getAmount(),
                payment.getPayeeName(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
