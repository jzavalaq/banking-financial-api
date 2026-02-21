package com.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Audit log entity for tracking all state-changing operations.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityType;

    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public enum ActionType {
        CREATE,
        UPDATE,
        DELETE,
        LOGIN,
        LOGOUT,
        TRANSFER,
        DEPOSIT,
        WITHDRAWAL,
        APPROVE,
        REJECT
    }
}
