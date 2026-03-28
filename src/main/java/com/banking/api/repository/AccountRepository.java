package com.banking.api.repository;

import com.banking.api.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Account entity.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(Long customerId);

    Page<Account> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.customer.id = :customerId AND a.status = :status")
    List<Account> findByCustomerIdAndStatus(@Param("customerId") Long customerId,
                                            @Param("status") Account.AccountStatus status);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.customer.id = :customerId AND a.status = 'ACTIVE'")
    long countActiveAccountsByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.customer.id = :customerId AND a.status = 'ACTIVE'")
    java.math.BigDecimal getTotalBalanceByCustomerId(@Param("customerId") Long customerId);
}
