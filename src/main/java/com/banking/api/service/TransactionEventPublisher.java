package com.banking.api.service;

import com.banking.api.entity.Transaction;
import com.banking.api.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for publishing transaction events to Kafka.
 *
 * <p>This service provides a layer of abstraction over Kafka publishing,
 * handling graceful degradation when Kafka is not available.</p>
 *
 * <p>In production with Kafka enabled, events are published to the
 * {@code banking.transactions} topic for downstream consumers.</p>
 *
 * <p>In development without Kafka, events are logged instead of published,
 * allowing the application to run without external dependencies.</p>
 */
@Service
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final boolean kafkaEnabled;

    public TransactionEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, TransactionEvent> kafkaTemplate,
            @org.springframework.beans.factory.annotation.Value("${kafka.enabled:false}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
    }

    /**
     * Publishes a transaction event to Kafka.
     *
     * <p>If Kafka is not enabled or unavailable, the event is logged
     * instead of being published, ensuring the application remains functional.</p>
     *
     * @param transaction the transaction entity to publish as an event
     * @param customerId  the customer ID associated with the account
     */
    public void publishTransactionEvent(Transaction transaction, Long customerId) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(transaction.getType().name())
                .transactionReference(transaction.getTransactionReference())
                .accountNumber(transaction.getAccount().getAccountNumber())
                .relatedAccountNumber(transaction.getRelatedAccount() != null
                        ? transaction.getRelatedAccount().getAccountNumber()
                        : null)
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .currency("USD")
                .status(transaction.getStatus().name())
                .description(transaction.getDescription())
                .timestamp(transaction.getCreatedAt())
                .customerId(customerId)
                .build();

        if (kafkaEnabled && kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TransactionEvent.TOPIC, event.transactionReference(), event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish transaction event: {}", event.transactionReference(), ex);
                            } else {
                                log.debug("Published transaction event to partition {}: {}",
                                        result.getRecordMetadata().partition(),
                                        event.transactionReference());
                            }
                        });
                log.info("Publishing transaction event: type={}, reference={}, amount={}",
                        event.eventType(), event.transactionReference(), event.amount());
            } catch (Exception e) {
                log.error("Error publishing transaction event: {}", event.transactionReference(), e);
            }
        } else {
            log.debug("Kafka disabled - logging transaction event: type={}, reference={}, amount={}",
                    event.eventType(), event.transactionReference(), event.amount());
        }
    }
}
