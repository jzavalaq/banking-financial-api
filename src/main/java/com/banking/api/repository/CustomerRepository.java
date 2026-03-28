package com.banking.api.repository;

import com.banking.api.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Customer entity.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByTaxId(String taxId);

    Optional<Customer> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByTaxId(String taxId);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.phone LIKE CONCAT('%', :search, '%')")
    Page<Customer> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.kycStatus = :status")
    Page<Customer> findByKycStatus(@Param("status") Customer.KycStatus status, Pageable pageable);
}
