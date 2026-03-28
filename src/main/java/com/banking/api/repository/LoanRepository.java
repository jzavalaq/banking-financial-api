package com.banking.api.repository;

import com.banking.api.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Loan entity.
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    boolean existsByLoanNumber(String loanNumber);

    List<Loan> findByCustomerId(Long customerId);

    Page<Loan> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.status = :status")
    List<Loan> findByStatus(@Param("status") Loan.LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.status IN ('PENDING', 'APPROVED')")
    List<Loan> findPendingApprovals();

    @Query("SELECT SUM(l.outstandingBalance) FROM Loan l WHERE l.customer.id = :customerId " +
           "AND l.status IN ('DISBURSED', 'ACTIVE')")
    BigDecimal getTotalOutstandingByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.customer.id = :customerId AND l.status = 'ACTIVE'")
    long countActiveLoansByCustomerId(@Param("customerId") Long customerId);
}
