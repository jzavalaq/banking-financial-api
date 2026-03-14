package com.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction entity representing a financial transaction.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String transactionReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_account_id")
    private Account relatedAccount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String category;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER_IN,
        TRANSFER_OUT,
        INTEREST,
        FEE
    }

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REVERSED
    }
}
