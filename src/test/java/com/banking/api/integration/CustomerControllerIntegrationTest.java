package com.banking.api.integration;

import com.banking.api.dto.CustomerDTOs.*;
import com.banking.api.entity.Customer;
import com.banking.api.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    private CreateCustomerRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CreateCustomerRequest(
                "John",
                "Doe",
                "john.integration" + System.currentTimeMillis() + "@test.com",
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
    @DisplayName("POST /api/v1/customers - should create customer with valid data")
    @WithMockUser(roles = {"TELLER"})
    void createCustomer_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value(createRequest.email()))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/customers - should return 409 for duplicate email")
    @WithMockUser(roles = {"TELLER"})
    void createCustomer_duplicateEmail_returns409() throws Exception {
        // Create first customer
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Try to create with same email
        CreateCustomerRequest duplicateRequest = new CreateCustomerRequest(
                "Jane", "Smith", createRequest.email(),
                "+1987654321", "456 Oak Ave", "Boston", "MA", "02101", "USA",
                LocalDate.of(1985, 5, 20), "987654321"
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/customers - should return 403 without authentication")
    void createCustomer_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/customers - should return 403 for CUSTOMER role")
    @WithMockUser(roles = {"CUSTOMER"})
    void createCustomer_forbiddenForCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - should return customer by ID")
    @WithMockUser(roles = {"TELLER"})
    void getCustomer_existingId_returns200() throws Exception {
        // Create customer first
        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(response, CustomerResponse.class);

        // Get customer by ID
        mockMvc.perform(get("/api/v1/customers/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.email").value(createRequest.email()));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - should return 404 for non-existent ID")
    @WithMockUser(roles = {"TELLER"})
    void getCustomer_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} - should update customer")
    @WithMockUser(roles = {"TELLER"})
    void updateCustomer_validRequest_returns200() throws Exception {
        // Create customer first
        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(response, CustomerResponse.class);

        // Update customer
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                "Jane", null, null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/customers/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("GET /api/v1/customers - should list customers with pagination")
    @WithMockUser(roles = {"TELLER"})
    void listCustomers_returnsPagedResults() throws Exception {
        // Create a customer
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // List customers
        mockMvc.perform(get("/api/v1/customers")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/customers/search - should search customers")
    @WithMockUser(roles = {"TELLER"})
    void searchCustomers_returnsMatchingResults() throws Exception {
        // Create a customer
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Search customers
        mockMvc.perform(get("/api/v1/customers/search")
                        .param("q", "John")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
