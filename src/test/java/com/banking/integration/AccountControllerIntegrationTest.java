package com.banking.integration;

import com.banking.dto.AccountDTOs.*;
import com.banking.dto.CustomerDTOs.*;
import com.banking.entity.Account;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long customerId;
    private String customerEmail;

    @BeforeEach
    void setUp() throws Exception {
        customerEmail = "account.integration" + System.currentTimeMillis() + "@test.com";
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                "John", "Doe", customerEmail,
                "+1234567890", "123 Main St", "New York", "NY", "10001", "USA",
                LocalDate.of(1990, 1, 15), "123456789"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        CustomerResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CustomerResponse.class);
        customerId = response.id();
    }

    @Test
    @DisplayName("POST /api/v1/accounts - should create checking account")
    @WithMockUser(roles = {"TELLER"})
    void createAccount_validRequest_returns201() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                Account.AccountType.CHECKING,
                new BigDecimal("1000.00"),
                new BigDecimal("500.00")
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/accounts - should return 404 for non-existent customer")
    @WithMockUser(roles = {"TELLER"})
    void createAccount_nonExistentCustomer_returns404() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(
                999999L,
                Account.AccountType.CHECKING,
                new BigDecimal("1000.00"),
                null
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{id} - should return account by ID")
    @WithMockUser(roles = {"CUSTOMER"})
    void getAccount_existingId_returns200() throws Exception {
        // Create account first
        CreateAccountRequest createRequest = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, null, null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), AccountResponse.class);

        // Get account by ID
        mockMvc.perform(get("/api/v1/accounts/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{id} - should return 404 for non-existent account")
    @WithMockUser(roles = {"CUSTOMER"})
    void getAccount_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/number/{accountNumber} - should return account by number")
    @WithMockUser(roles = {"CUSTOMER"})
    void getAccountByNumber_existingNumber_returns200() throws Exception {
        // Create account first
        CreateAccountRequest createRequest = new CreateAccountRequest(
                customerId, Account.AccountType.SAVINGS, null, null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), AccountResponse.class);

        // Get account by number
        mockMvc.perform(get("/api/v1/accounts/number/{accountNumber}", created.accountNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(created.accountNumber()));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNumber}/balance - should return balance")
    @WithMockUser(roles = {"CUSTOMER"})
    void getBalance_existingAccount_returns200() throws Exception {
        // Create account first
        CreateAccountRequest createRequest = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, new BigDecimal("1000.00"), new BigDecimal("500.00")
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), AccountResponse.class);

        // Get balance
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}/balance", created.accountNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(created.accountNumber()))
                .andExpect(jsonPath("$.currentBalance").value(1000.00))
                .andExpect(jsonPath("$.availableBalance").value(1500.00));
    }

    @Test
    @DisplayName("PUT /api/v1/accounts/{id}/status - should freeze account")
    @WithMockUser(roles = {"MANAGER"})
    void updateAccountStatus_freezeAccount_returns200() throws Exception {
        // Create account first
        CreateAccountRequest createRequest = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, null, null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), AccountResponse.class);

        // Freeze account
        UpdateAccountStatusRequest statusRequest = new UpdateAccountStatusRequest(
                Account.AccountStatus.FROZEN, "Suspicious activity"
        );

        mockMvc.perform(put("/api/v1/accounts/{id}/status", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    @DisplayName("PUT /api/v1/accounts/{id}/status - should return 400 when closing account with balance")
    @WithMockUser(roles = {"MANAGER"})
    void updateAccountStatus_closeWithBalance_returns400() throws Exception {
        // Create account with balance
        CreateAccountRequest createRequest = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, new BigDecimal("100.00"), null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), AccountResponse.class);

        // Try to close account
        UpdateAccountStatusRequest statusRequest = new UpdateAccountStatusRequest(
                Account.AccountStatus.CLOSED, null
        );

        mockMvc.perform(put("/api/v1/accounts/{id}/status", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/accounts/{id}/status - should return 403 for TELLER role")
    @WithMockUser(roles = {"TELLER"})
    void updateAccountStatus_forbiddenForTeller_returns403() throws Exception {
        UpdateAccountStatusRequest statusRequest = new UpdateAccountStatusRequest(
                Account.AccountStatus.FROZEN, "Test"
        );

        mockMvc.perform(put("/api/v1/accounts/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/customer/{customerId} - should return accounts for customer")
    @WithMockUser(roles = {"CUSTOMER"})
    void getAccountsByCustomer_returnsPagedResults() throws Exception {
        // Create two accounts
        CreateAccountRequest request1 = new CreateAccountRequest(customerId, Account.AccountType.CHECKING, null, null);
        CreateAccountRequest request2 = new CreateAccountRequest(customerId, Account.AccountType.SAVINGS, null, null);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teller").roles("TELLER")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("teller").roles("TELLER")))
                .andExpect(status().isCreated());

        // Get accounts by customer
        mockMvc.perform(get("/api/v1/accounts/customer/{customerId}", customerId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)));
    }
}
