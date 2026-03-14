package com.banking.service;

import com.banking.dto.CustomerDTOs.*;
import com.banking.dto.PagedResponse;
import com.banking.entity.Customer;
import com.banking.entity.Role;
import com.banking.entity.User;
import com.banking.exception.DuplicateResourceException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.CustomerRepository;
import com.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for Customer operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.email());

        // Check for duplicates
        if (customerRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Customer", "email", request.email());
        }
        if (customerRepository.existsByTaxId(request.taxId())) {
            throw new DuplicateResourceException("Customer", "taxId", request.taxId());
        }

        // Create user account for customer
        User user = User.builder()
                .username(generateUsername(request.firstName(), request.lastName()))
                .password(passwordEncoder.encode(UUID.randomUUID().toString().substring(0, 12)))
                .email(request.email())
                .role(Role.CUSTOMER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        user = userRepository.save(user);

        // Create customer
        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .zipCode(request.zipCode())
                .country(request.country())
                .dateOfBirth(request.dateOfBirth())
                .taxId(request.taxId())
                .kycStatus(Customer.KycStatus.PENDING)
                .user(user)
                .build();

        customer = customerRepository.save(customer);
        log.info("Created customer with ID: {}", customer.getId());

        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        log.info("Updating customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        if (request.firstName() != null) {
            customer.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            customer.setLastName(request.lastName());
        }
        if (request.email() != null && !request.email().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Customer", "email", request.email());
            }
            customer.setEmail(request.email());
        }
        if (request.phone() != null) {
            customer.setPhone(request.phone());
        }
        if (request.address() != null) {
            customer.setAddress(request.address());
        }
        if (request.city() != null) {
            customer.setCity(request.city());
        }
        if (request.state() != null) {
            customer.setState(request.state());
        }
        if (request.zipCode() != null) {
            customer.setZipCode(request.zipCode());
        }
        if (request.country() != null) {
            customer.setCountry(request.country());
        }

        customer = customerRepository.save(customer);
        log.info("Updated customer with ID: {}", customer.getId());

        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerSummary> listCustomers(int page, int size, String sortBy, String sortDir) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, safeSize, sort);

        Page<Customer> customerPage = customerRepository.findAll(pageable);

        return new PagedResponse<>(
                customerPage.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                customerPage.getNumber(),
                customerPage.getSize(),
                customerPage.getTotalElements(),
                customerPage.getTotalPages(),
                customerPage.isFirst(),
                customerPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerSummary> searchCustomers(String search, int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());

        Page<Customer> customerPage = customerRepository.search(search, pageable);

        return new PagedResponse<>(
                customerPage.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                customerPage.getNumber(),
                customerPage.getSize(),
                customerPage.getTotalElements(),
                customerPage.getTotalPages(),
                customerPage.isFirst(),
                customerPage.isLast()
        );
    }

    private String generateUsername(String firstName, String lastName) {
        String base = (firstName.substring(0, 1) + lastName).toLowerCase().replaceAll("[^a-z]", "");
        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        return username;
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getCity(),
                customer.getState(),
                customer.getZipCode(),
                customer.getCountry(),
                customer.getDateOfBirth(),
                customer.getTaxId(),
                customer.getKycStatus(),
                customer.getVersion(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    private CustomerSummary toSummary(Customer customer) {
        return new CustomerSummary(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getKycStatus()
        );
    }
}
