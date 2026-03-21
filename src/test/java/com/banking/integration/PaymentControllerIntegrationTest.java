package com.banking.integration;

import com.banking.dto.AccountDTOs.*;
import com.banking.dto.CustomerDTOs.*;
import com.banking.dto.PaymentDTOs.*;
import com.banking.entity.Account;
import com.banking.entity.Payment;
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
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accountNumber;

    @BeforeEach
    void setUp() throws Exception {
        // Create customer
        String customerEmail = "payment.integration" + System.currentTimeMillis() + "@test.com";
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

        // Create account
        CreateAccountRequest accountRequest = new CreateAccountRequest(
                customer.id(), Account.AccountType.CHECKING, new BigDecimal("1000.00"), null
        );

        MvcResult accountResult = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accountRequest))
                        .with(user("teller").roles("TELLER")))
                .andExpect(status().isCreated())
                .andReturn();

        AccountResponse account = objectMapper.readValue(accountResult.getResponse().getContentAsString(), AccountResponse.class);
        accountNumber = account.accountNumber();
    }

    @Test
    @DisplayName("POST /api/v1/payments - should create bill payment")
    @WithMockUser(roles = {"CUSTOMER"})
    void createPayment_validRequest_returns201() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber,
                Payment.PaymentType.BILL,
                new BigDecimal("100.00"),
                "Electric Company",
                "ELEC123456",
                "Bill-123",
                null,
                "Monthly electricity bill"
        );

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").exists())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should create scheduled payment")
    @WithMockUser(roles = {"CUSTOMER"})
    void createPayment_scheduledPayment_returns201WithPendingStatus() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber,
                Payment.PaymentType.BILL,
                new BigDecimal("100.00"),
                "Gas Company",
                "GAS123456",
                "Bill-456",
                LocalDate.now().plusDays(7),
                "Scheduled gas bill"
        );

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentReference").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - should return 422 for insufficient funds")
    @WithMockUser(roles = {"CUSTOMER"})
    void createPayment_insufficientFunds_returns422() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber,
                Payment.PaymentType.BILL,
                new BigDecimal("5000.00"),
                "Test Payee",
                "TEST123",
                "REF123",
                null,
                "Test"
        );

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - should return payment by ID")
    @WithMockUser(roles = {"CUSTOMER"})
    void getPayment_existingId_returns200() throws Exception {
        // Create payment first
        CreatePaymentRequest createRequest = new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("50.00"),
                "Test Payee", "TEST123", "REF123", null, "Test"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), PaymentResponse.class);

        // Get payment by ID
        mockMvc.perform(get("/api/v1/payments/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - should return 404 for non-existent payment")
    @WithMockUser(roles = {"CUSTOMER"})
    void getPayment_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/payments/{id} - should cancel pending payment")
    @WithMockUser(roles = {"CUSTOMER"})
    void cancelPayment_pendingPayment_returns204() throws Exception {
        // Create scheduled payment
        CreatePaymentRequest createRequest = new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("100.00"),
                "Test Payee", "TEST123", "REF123", LocalDate.now().plusDays(7), "Scheduled"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), PaymentResponse.class);

        // Cancel payment - returns 204 NO_CONTENT
        mockMvc.perform(delete("/api/v1/payments/{id}", created.id()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/payments/{id} - should return 400 for completed payment")
    @WithMockUser(roles = {"CUSTOMER"})
    void cancelPayment_completedPayment_returns400() throws Exception {
        // Create immediate payment (completed)
        CreatePaymentRequest createRequest = new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("50.00"),
                "Test Payee", "TEST123", "REF123", null, "Immediate"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), PaymentResponse.class);

        // Try to cancel completed payment
        mockMvc.perform(delete("/api/v1/payments/{id}", created.id()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/payments/account/{accountNumber} - should return payments for account")
    @WithMockUser(roles = {"CUSTOMER"})
    void getPaymentsByAccount_returnsPagedResults() throws Exception {
        // Create payments
        CreatePaymentRequest request1 = new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("50.00"),
                "Payee1", "ACCT1", "REF1", null, null
        );
        CreatePaymentRequest request2 = new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("75.00"),
                "Payee2", "ACCT2", "REF2", null, null
        );

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated());

        // Get payments by account
        mockMvc.perform(get("/api/v1/payments/account/{accountNumber}", accountNumber)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)));
    }
}
