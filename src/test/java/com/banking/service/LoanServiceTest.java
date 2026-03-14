package com.banking.service;

import com.banking.dto.AccountDTOs.*;
import com.banking.dto.CustomerDTOs.*;
import com.banking.dto.LoanDTOs.*;
import com.banking.entity.Account;
import com.banking.entity.Loan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class LoanServiceTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CustomerService customerService;

    private Long customerId;
    private String accountNumber;

    @BeforeEach
    void setUp() {
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                "John",
                "Doe",
                "john.loan" + System.currentTimeMillis() + "@test.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA",
                LocalDate.of(1990, 1, 15),
                "123456789"
        );

        customerId = customerService.createCustomer(customerRequest).id();

        CreateAccountRequest accountRequest = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, new BigDecimal("1000.00"), null);
        accountNumber = accountService.createAccount(accountRequest).accountNumber();
    }

    @Test
    @DisplayName("Should apply for loan")
    void shouldApplyForLoan() {
        LoanApplicationRequest request = new LoanApplicationRequest(
                customerId,
                accountNumber,
                Loan.LoanType.PERSONAL,
                new BigDecimal("10000.00"),
                24,
                "Home renovation"
        );

        LoanResponse response = loanService.applyForLoan(request);

        assertNotNull(response.loanNumber());
        assertEquals(Loan.LoanStatus.PENDING, response.status());
        assertNotNull(response.monthlyPayment());
    }

    @Test
    @DisplayName("Should approve loan")
    void shouldApproveLoan() {
        LoanResponse loan = loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        LoanApprovalRequest approvalRequest = new LoanApprovalRequest(
                true,
                new BigDecimal("0.08"),
                null
        );

        LoanResponse approved = loanService.approveLoan(loan.id(), approvalRequest);

        assertEquals(Loan.LoanStatus.APPROVED, approved.status());
        assertNotNull(approved.approvalDate());
    }

    @Test
    @DisplayName("Should reject loan application")
    void shouldRejectLoanApplication() {
        LoanResponse loan = loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        LoanApprovalRequest rejectionRequest = new LoanApprovalRequest(
                false,
                null,
                "Insufficient credit history"
        );

        LoanResponse rejected = loanService.approveLoan(loan.id(), rejectionRequest);

        assertEquals(Loan.LoanStatus.REJECTED, rejected.status());
    }

    @Test
    @DisplayName("Should disburse approved loan")
    void shouldDisburseApprovedLoan() {
        LoanResponse loan = loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        loanService.approveLoan(loan.id(), new LoanApprovalRequest(true, null, null));

        LoanDisbursementRequest disbursementRequest = new LoanDisbursementRequest("Disbursed to account");
        LoanResponse disbursed = loanService.disburseLoan(loan.id(), disbursementRequest);

        assertEquals(Loan.LoanStatus.DISBURSED, disbursed.status());
        assertNotNull(disbursed.disbursementDate());

        var balance = accountService.getBalance(accountNumber);
        assertEquals(new BigDecimal("6000.00"), balance.currentBalance());
    }

    @Test
    @DisplayName("Should list loans by customer")
    void shouldListLoansByCustomer() {
        loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        var loans = loanService.getLoansByCustomerId(customerId);

        assertFalse(loans.isEmpty());
    }

    @Test
    @DisplayName("Should calculate interest correctly")
    void shouldCalculateInterestCorrectly() {
        LoanApplicationRequest request = new LoanApplicationRequest(
                customerId,
                accountNumber,
                Loan.LoanType.MORTGAGE,
                new BigDecimal("100000.00"),
                360,
                "Home purchase"
        );

        LoanResponse response = loanService.applyForLoan(request);

        assertEquals(new BigDecimal("0.065"), response.interestRate());
        assertNotNull(response.monthlyPayment());
        assertTrue(response.monthlyPayment().compareTo(BigDecimal.ZERO) > 0);
    }
}
