package com.banking.api.integration;

import com.banking.api.dto.AccountDTOs.*;
import com.banking.api.dto.CustomerDTOs.*;
import com.banking.api.dto.TransactionDTOs.*;
import com.banking.api.entity.Account;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accountNumber1;
    private String accountNumber2;

    @BeforeEach
    void setUp() throws Exception {
        // Create customer
        String customerEmail = "txn.integration" + System.currentTimeMillis() + "@test.com";
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                "John", "Doe", customerEmail,
                "+1234567890", "123 Main St", "New York", "NY", "10001", "USA",
                LocalDate.of(1990, 1, 15), "123456789"
        );

        MvcResult customerResult = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest))
                        .with(user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        CustomerResponse customer = objectMapper.readValue(customerResult.getResponse().getContentAsString(), CustomerResponse.class);

        // Create first account
        CreateAccountRequest accountRequest1 = new CreateAccountRequest(
                customer.id(), Account.AccountType.CHECKING, new BigDecimal("1000.00"), null
        );

        MvcResult accountResult1 = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest1))
                        .with(user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse account1 = objectMapper.readValue(accountResult1.getResponse().getContentAsString(), AccountResponse.class);
        accountNumber1 = account1.accountNumber();

        // Create second account
        CreateAccountRequest accountRequest2 = new CreateAccountRequest(
                customer.id(), Account.AccountType.SAVINGS, new BigDecimal("500.00"), null
        );

        MvcResult accountResult2 = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest2))
                        .with(user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse account2 = objectMapper.readValue(accountResult2.getResponse().getContentAsString(), AccountResponse.class);
        accountNumber2 = account2.accountNumber();
    }

    @Test
    @DisplayName("POST /api/v1/transactions/deposit - should deposit to account")
    @WithMockUser(roles = {"TELLER"})
    void deposit_validRequest_returns201() throws Exception {
        DepositRequest request = new DepositRequest(accountNumber1, new BigDecimal("500.00"), "Test deposit");

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionReference").exists())
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.balanceAfter").value(1500.00));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/deposit - should return 404 for non-existent account")
    @WithMockUser(roles = {"TELLER"})
    void deposit_nonExistentAccount_returns404() throws Exception {
        DepositRequest request = new DepositRequest("BA999999", new BigDecimal("100.00"), "Test");

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/transactions/withdraw - should withdraw from account")
    @WithMockUser(roles = {"TELLER"})
    void withdraw_validRequest_returns201() throws Exception {
        WithdrawalRequest request = new WithdrawalRequest(accountNumber1, new BigDecimal("200.00"), "Test withdrawal");

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionReference").exists())
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.balanceAfter").value(800.00));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/withdraw - should return 422 for insufficient funds")
    @WithMockUser(roles = {"TELLER"})
    void withdraw_insufficientFunds_returns422() throws Exception {
        WithdrawalRequest request = new WithdrawalRequest(accountNumber1, new BigDecimal("5000.00"), "Test withdrawal");

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/v1/transactions/transfer - should transfer between accounts")
    @WithMockUser(roles = {"CUSTOMER"})
    void transfer_validRequest_returns201() throws Exception {
        TransferRequest request = new TransferRequest(accountNumber1, accountNumber2, new BigDecimal("300.00"), "Test transfer");

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionReference").exists())
                .andExpect(jsonPath("$.amount").value(300.00));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/transfer - should return 400 for same account transfer")
    @WithMockUser(roles = {"CUSTOMER"})
    void transfer_sameAccount_returns400() throws Exception {
        TransferRequest request = new TransferRequest(accountNumber1, accountNumber1, new BigDecimal("100.00"), "Test");

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/transactions/transfer - should return 422 for insufficient funds")
    @WithMockUser(roles = {"CUSTOMER"})
    void transfer_insufficientFunds_returns422() throws Exception {
        TransferRequest request = new TransferRequest(accountNumber1, accountNumber2, new BigDecimal("5000.00"), "Test");

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /api/v1/transactions/account/{accountNumber} - should return transaction history")
    @WithMockUser(roles = {"CUSTOMER"})
    void getTransactionHistory_returnsPagedResults() throws Exception {
        // Create a transaction first
        DepositRequest depositRequest = new DepositRequest(accountNumber1, new BigDecimal("100.00"), "Test");
        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest))
                        .with(user("teller").roles("TELLER")))
                .andExpect(status().isCreated());

        // Get transaction history
        mockMvc.perform(get("/api/v1/transactions/account/{accountNumber}", accountNumber1)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }
}
