package com.banking.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Account entity representing a bank account.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(precision = 19, scale = 4)
    private BigDecimal interestRate;

    @Column(precision = 19, scale = 4)
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum AccountType {
        CHECKING,
        SAVINGS
    }

    public enum AccountStatus {
        ACTIVE,
        FROZEN,
        CLOSED
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean hasSufficientFunds(BigDecimal amount) {
        return balance.add(overdraftLimit).compareTo(amount) >= 0;
    }
}
