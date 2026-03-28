package com.banking.api.service;

import com.banking.api.dto.AccountDTOs.*;
import com.banking.api.dto.CustomerDTOs.*;
import com.banking.api.dto.LoanDTOs.*;
import com.banking.api.entity.Account;
import com.banking.api.entity.Loan;
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

    @Test
    @DisplayName("Should throw when applying for loan with non-existent customer")
    void shouldThrowWhenApplyingForLoanWithNonExistentCustomer() {
        LoanApplicationRequest request = new LoanApplicationRequest(
                999999L, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        assertThrows(Exception.class, () -> loanService.applyForLoan(request));
    }

    @Test
    @DisplayName("Should throw when applying for loan with non-existent account")
    void shouldThrowWhenApplyingForLoanWithNonExistentAccount() {
        LoanApplicationRequest request = new LoanApplicationRequest(
                customerId, "BA999999", Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        assertThrows(Exception.class, () -> loanService.applyForLoan(request));
    }

    @Test
    @DisplayName("Should throw when disbursement account belongs to different customer")
    void shouldThrowWhenDisbursementAccountBelongsToDifferentCustomer() {
        // Create another customer
        CreateCustomerRequest anotherCustomerRequest = new CreateCustomerRequest(
                "Jane", "Smith", "jane.loan" + System.currentTimeMillis() + "@test.com",
                "+1987654321", "456 Oak Ave", "Boston", "MA", "02101", "USA",
                LocalDate.of(1985, 5, 20), "999999999"
        );
        Long anotherCustomerId = customerService.createCustomer(anotherCustomerRequest).id();

        // Try to apply for loan with account from different customer
        LoanApplicationRequest request = new LoanApplicationRequest(
                anotherCustomerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        assertThrows(Exception.class, () -> loanService.applyForLoan(request));
    }

    @Test
    @DisplayName("Should throw when approving non-existent loan")
    void shouldThrowWhenApprovingNonExistentLoan() {
        LoanApprovalRequest request = new LoanApprovalRequest(true, null, null);

        assertThrows(Exception.class, () -> loanService.approveLoan(999999L, request));
    }

    @Test
    @DisplayName("Should throw when approving already approved loan")
    void shouldThrowWhenApprovingAlreadyApprovedLoan() {
        LoanResponse loan = loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        loanService.approveLoan(loan.id(), new LoanApprovalRequest(true, null, null));

        assertThrows(Exception.class, () ->
                loanService.approveLoan(loan.id(), new LoanApprovalRequest(true, null, null)));
    }

    @Test
    @DisplayName("Should throw when disbursing non-existent loan")
    void shouldThrowWhenDisbursingNonExistentLoan() {
        LoanDisbursementRequest request = new LoanDisbursementRequest("Test");

        assertThrows(Exception.class, () -> loanService.disburseLoan(999999L, request));
    }

    @Test
    @DisplayName("Should throw when disbursing pending loan")
    void shouldThrowWhenDisbursingPendingLoan() {
        LoanResponse loan = loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        LoanDisbursementRequest request = new LoanDisbursementRequest("Test");

        assertThrows(Exception.class, () -> loanService.disburseLoan(loan.id(), request));
    }

    @Test
    @DisplayName("Should throw when disbursing already disbursed loan")
    void shouldThrowWhenDisbursingAlreadyDisbursedLoan() {
        LoanResponse loan = loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        loanService.approveLoan(loan.id(), new LoanApprovalRequest(true, null, null));
        loanService.disburseLoan(loan.id(), new LoanDisbursementRequest("Test"));

        assertThrows(Exception.class, () ->
                loanService.disburseLoan(loan.id(), new LoanDisbursementRequest("Test")));
    }

    @Test
    @DisplayName("Should get loan by number")
    void shouldGetLoanByNumber() {
        LoanResponse loan = loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        LoanResponse found = loanService.getLoanByNumber(loan.loanNumber());

        assertEquals(loan.id(), found.id());
        assertEquals(loan.loanNumber(), found.loanNumber());
    }

    @Test
    @DisplayName("Should throw when loan number not found")
    void shouldThrowWhenLoanNumberNotFound() {
        assertThrows(Exception.class, () -> loanService.getLoanByNumber("LN999999"));
    }

    @Test
    @DisplayName("Should get paginated loans by customer")
    void shouldGetPaginatedLoansByCustomer() {
        loanService.applyForLoan(new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null));

        var response = loanService.getLoansByCustomerIdPaged(customerId, 0, 10);

        assertNotNull(response.content());
        assertTrue(response.totalElements() >= 1);
    }

    @Test
    @DisplayName("Should throw when applying for loan with frozen account")
    void shouldThrowWhenApplyingForLoanWithFrozenAccount() {
        // Freeze the account
        var account = accountService.getAccountByNumber(accountNumber);
        accountService.updateAccountStatus(account.id(),
                new UpdateAccountStatusRequest(Account.AccountStatus.FROZEN, "Test"));

        LoanApplicationRequest request = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.PERSONAL,
                new BigDecimal("5000.00"), 12, null
        );

        assertThrows(Exception.class, () -> loanService.applyForLoan(request));
    }

    @Test
    @DisplayName("Should apply for auto loan")
    void shouldApplyForAutoLoan() {
        LoanApplicationRequest request = new LoanApplicationRequest(
                customerId, accountNumber, Loan.LoanType.AUTO,
                new BigDecimal("25000.00"), 48, "Car purchase"
        );

        LoanResponse response = loanService.applyForLoan(request);

        assertEquals(Loan.LoanType.AUTO, response.loanType());
        assertEquals(new BigDecimal("0.085"), response.interestRate());
    }
}
