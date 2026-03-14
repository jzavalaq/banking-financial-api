package com.banking.service;

import com.banking.dto.CustomerDTOs.*;
import com.banking.entity.Customer;
import com.banking.entity.Role;
import com.banking.entity.User;
import com.banking.exception.DuplicateResourceException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.CustomerRepository;
import com.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CreateCustomerRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CreateCustomerRequest(
                "John",
                "Doe",
                "john.doe" + System.currentTimeMillis() + "@test.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA",
                LocalDate.of(1990, 1, 15),
                "123456789"
        );
    }

    @Test
    @DisplayName("Should create customer with valid data")
    void shouldCreateCustomer() {
        CustomerResponse response = customerService.createCustomer(createRequest);

        assertNotNull(response.id());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertEquals("John Doe", response.fullName());
        assertEquals(Customer.KycStatus.PENDING, response.kycStatus());
        assertNotNull(response.createdAt());
    }

    @Test
    @DisplayName("Should reject duplicate email")
    void shouldRejectDuplicateEmail() {
        customerService.createCustomer(createRequest);

        CreateCustomerRequest duplicateRequest = new CreateCustomerRequest(
                "Jane",
                "Smith",
                createRequest.email(),
                "+1987654321",
                "456 Oak Ave",
                "Boston",
                "MA",
                "02101",
                "USA",
                LocalDate.of(1985, 5, 20),
                "987654321"
        );

        assertThrows(DuplicateResourceException.class, () ->
                customerService.createCustomer(duplicateRequest));
    }

    @Test
    @DisplayName("Should return customer by ID")
    void shouldReturnCustomerById() {
        CustomerResponse created = customerService.createCustomer(createRequest);
        CustomerResponse found = customerService.getCustomerById(created.id());

        assertEquals(created.id(), found.id());
        assertEquals(created.email(), found.email());
    }

    @Test
    @DisplayName("Should throw when customer not found")
    void shouldThrowWhenCustomerNotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                customerService.getCustomerById(999999L));
    }

    @Test
    @DisplayName("Should update customer profile")
    void shouldUpdateCustomerProfile() {
        CustomerResponse created = customerService.createCustomer(createRequest);

        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                "Jane",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        CustomerResponse updated = customerService.updateCustomer(created.id(), updateRequest);

        assertEquals("Jane", updated.firstName());
        assertEquals("Doe", updated.lastName());
    }

    @Test
    @DisplayName("Should list customers with pagination")
    void shouldListCustomersWithPagination() {
        customerService.createCustomer(createRequest);

        var response = customerService.listCustomers(0, 10, "createdAt", "desc");

        assertNotNull(response.content());
        assertTrue(response.totalElements() >= 1);
    }
}
