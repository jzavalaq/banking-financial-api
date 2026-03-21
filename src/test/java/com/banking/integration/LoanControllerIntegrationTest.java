package com.banking.integration;

import com.banking.dto.AccountDTOs.*;
import com.banking.dto.CustomerDTOs.*;
import com.banking.dto.LoanDTOs.*;
import com.banking.entity.Account;
import com.banking.entity.Loan;
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
class LoanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long customerId;
    private String accountNumber;

    @BeforeEach
    void setUp() throws Exception {
        // Create customer
        String customerEmail = "loan.integration" + System.currentTimeMillis() + "@test.com";
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
        customerId = customer.id();

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
    @DisplayName("POST /api/v1/loans - should apply for loan")
    @WithMockUser(roles = {"CUSTOMER"})
    void applyForLoan_validRequest_returns201() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest(
                customerId,
                accountNumber,
                Loan.LoanType.PERSONAL,
                new BigDecimal("10000.00"),
                24,
                "Home renovation"
        );

        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanNumber").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.monthlyPayment").exists());
    }

    @Test
    @DisplayName("POST /api/v1/loans - should return 404 for non-existent customer")
    @WithMockUser(roles = {"CUSTOMER"})
    void applyForLoan_nonExistentCustomer_returns404() throws Exception {
        LoanApplicationRequest request = new LoanApplicationRequest(
                999999L, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("10000.00"), 24, "Test"
        );

        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/loans/{id}/approve - should approve loan")
    @WithMockUser(roles = {"MANAGER"})
    void approveLoan_approveRequest_returns200() throws Exception {
        // Apply for loan first
        LoanApplicationRequest applyRequest = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        MvcResult applyResult = mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        LoanResponse loan = objectMapper.readValue(applyResult.getResponse().getContentAsString(), LoanResponse.class);

        // Approve loan
        LoanApprovalRequest approvalRequest = new LoanApprovalRequest(true, new BigDecimal("0.08"), null);

        mockMvc.perform(put("/api/v1/loans/{id}/approve", loan.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvalDate").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/loans/{id}/approve - should reject loan")
    @WithMockUser(roles = {"MANAGER"})
    void approveLoan_rejectRequest_returns200() throws Exception {
        // Apply for loan first
        LoanApplicationRequest applyRequest = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        MvcResult applyResult = mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        LoanResponse loan = objectMapper.readValue(applyResult.getResponse().getContentAsString(), LoanResponse.class);

        // Reject loan
        LoanApprovalRequest rejectionRequest = new LoanApprovalRequest(false, null, "Insufficient credit history");

        mockMvc.perform(put("/api/v1/loans/{id}/approve", loan.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("PUT /api/v1/loans/{id}/approve - should return 400 for non-pending loan")
    @WithMockUser(roles = {"MANAGER"})
    void approveLoan_nonPendingLoan_returns400() throws Exception {
        // Apply for loan
        LoanApplicationRequest applyRequest = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        MvcResult applyResult = mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        LoanResponse loan = objectMapper.readValue(applyResult.getResponse().getContentAsString(), LoanResponse.class);

        // Approve loan
        LoanApprovalRequest approvalRequest = new LoanApprovalRequest(true, null, null);
        mockMvc.perform(put("/api/v1/loans/{id}/approve", loan.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest))
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());

        // Try to approve again
        mockMvc.perform(put("/api/v1/loans/{id}/approve", loan.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/loans/{id}/approve - should return 403 for TELLER role")
    @WithMockUser(roles = {"TELLER"})
    void approveLoan_forbiddenForTeller_returns403() throws Exception {
        LoanApprovalRequest approvalRequest = new LoanApprovalRequest(true, null, null);

        mockMvc.perform(put("/api/v1/loans/{id}/approve", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/loans/{id}/disburse - should disburse approved loan")
    @WithMockUser(roles = {"MANAGER"})
    void disburseLoan_approvedLoan_returns200() throws Exception {
        // Apply and approve loan
        LoanApplicationRequest applyRequest = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        MvcResult applyResult = mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        LoanResponse loan = objectMapper.readValue(applyResult.getResponse().getContentAsString(), LoanResponse.class);

        LoanApprovalRequest approvalRequest = new LoanApprovalRequest(true, null, null);
        mockMvc.perform(put("/api/v1/loans/{id}/approve", loan.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalRequest))
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());

        // Disburse loan
        LoanDisbursementRequest disbursementRequest = new LoanDisbursementRequest("Disbursed to account");

        mockMvc.perform(post("/api/v1/loans/{id}/disburse", loan.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disbursementRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISBURSED"))
                .andExpect(jsonPath("$.disbursementDate").exists());
    }

    @Test
    @DisplayName("POST /api/v1/loans/{id}/disburse - should return 400 for non-approved loan")
    @WithMockUser(roles = {"MANAGER"})
    void disburseLoan_nonApprovedLoan_returns400() throws Exception {
        // Apply for loan (still pending)
        LoanApplicationRequest applyRequest = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        MvcResult applyResult = mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        LoanResponse loan = objectMapper.readValue(applyResult.getResponse().getContentAsString(), LoanResponse.class);

        // Try to disburse
        LoanDisbursementRequest disbursementRequest = new LoanDisbursementRequest("Test");

        mockMvc.perform(post("/api/v1/loans/{id}/disburse", loan.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disbursementRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/loans/{id} - should return loan by ID")
    @WithMockUser(roles = {"CUSTOMER"})
    void getLoan_existingId_returns200() throws Exception {
        // Apply for loan
        LoanApplicationRequest applyRequest = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        MvcResult applyResult = mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn();

        LoanResponse loan = objectMapper.readValue(applyResult.getResponse().getContentAsString(), LoanResponse.class);

        // Get loan by ID
        mockMvc.perform(get("/api/v1/loans/{id}", loan.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.id()));
    }

    @Test
    @DisplayName("GET /api/v1/loans/{id} - should return 404 for non-existent loan")
    @WithMockUser(roles = {"CUSTOMER"})
    void getLoan_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/loans/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/loans/customer/{customerId} - should return loans for customer")
    @WithMockUser(roles = {"CUSTOMER"})
    void getLoansByCustomer_returnsPagedResults() throws Exception {
        // Apply for loan
        LoanApplicationRequest applyRequest = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(applyRequest))
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isCreated());

        // Get loans by customer
        mockMvc.perform(get("/api/v1/loans/customer/{customerId}", customerId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }
}
