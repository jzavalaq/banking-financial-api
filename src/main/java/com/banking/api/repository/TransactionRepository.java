package com.banking.api.repository;

import com.banking.api.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Transaction entity.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
           "AND t.type = :type ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndType(
            @Param("accountId") Long accountId,
            @Param("type") Transaction.TransactionType type,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.customer.id = :customerId " +
           "AND t.createdAt >= :since")
    long countRecentTransactionsByCustomerId(
            @Param("customerId") Long customerId,
            @Param("since") Instant since);
}
