package com.banking.api.repository;

import com.banking.api.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    Page<Payment> findByAccountId(Long accountId, Pageable pageable);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.account.id = :accountId " +
           "AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByAccountIdAndStatus(
            @Param("accountId") Long accountId,
            @Param("status") Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.scheduledDate = :date AND p.status = 'PENDING'")
    List<Payment> findScheduledPaymentsForDate(@Param("date") LocalDate date);

    @Query("SELECT p FROM Payment p WHERE p.account.customer.id = :customerId")
    Page<Payment> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);
}
