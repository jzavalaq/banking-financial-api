package com.banking.api.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event payload published to Kafka when a financial transaction occurs.
 *
 * <p>This event enables downstream systems to react to transaction events
 * in real-time, such as:</p>
 * <ul>
 *   <li>Notification service - send alerts to customers</li>
 *   <li>Analytics service - update dashboards and reports</li>
 *   <li>Fraud detection - analyze patterns for suspicious activity</li>
 *   <li>Audit service - maintain immutable transaction logs</li>
 * </ul>
 *
 * @param eventId            Unique identifier for this event
 * @param eventType          Type of transaction (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT)
 * @param transactionReference The business reference for the transaction
 * @param accountNumber      The account number involved in the transaction
 * @param relatedAccountNumber The counterparty account (for transfers), null otherwise
 * @param amount             The transaction amount
 * @param balanceAfter       The account balance after the transaction
 * @param currency           The currency code (default: USD)
 * @param status             The transaction status (COMPLETED, PENDING, FAILED)
 * @param description        Optional description of the transaction
 * @param timestamp          When the transaction occurred
 * @param customerId         The customer ID who owns the account
 */
public record TransactionEvent(
        String eventId,
        String eventType,
        String transactionReference,
        String accountNumber,
        String relatedAccountNumber,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String currency,
        String status,
        String description,
        Instant timestamp,
        Long customerId
) {
    public static final String TOPIC = "banking.transactions";

    /**
     * Creates a builder for constructing TransactionEvent instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String eventType;
        private String transactionReference;
        private String accountNumber;
        private String relatedAccountNumber;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String currency = "USD";
        private String status;
        private String description;
        private Instant timestamp;
        private Long customerId;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder relatedAccountNumber(String relatedAccountNumber) {
            this.relatedAccountNumber = relatedAccountNumber;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder balanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }

        public TransactionEvent build() {
            return new TransactionEvent(
                    eventId, eventType, transactionReference, accountNumber,
                    relatedAccountNumber, amount, balanceAfter, currency,
                    status, description, timestamp, customerId
            );
        }
    }
}
