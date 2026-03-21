package com.banking.service;

import com.banking.dto.AccountDTOs.*;
import com.banking.dto.CustomerDTOs.*;
import com.banking.dto.TransactionDTOs.*;
import com.banking.entity.Account;
import com.banking.exception.InsufficientFundsException;
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
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CustomerService customerService;

    private String accountNumber1;
    private String accountNumber2;

    @BeforeEach
    void setUp() {
        Long customerId = createCustomer();

        CreateAccountRequest request1 = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, new BigDecimal("1000.00"), null);
        accountNumber1 = accountService.createAccount(request1).accountNumber();

        CreateAccountRequest request2 = new CreateAccountRequest(
                customerId, Account.AccountType.SAVINGS, new BigDecimal("500.00"), null);
        accountNumber2 = accountService.createAccount(request2).accountNumber();
    }

    private Long createCustomer() {
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                "John",
                "Doe",
                "john.txn" + System.currentTimeMillis() + "@test.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA",
                LocalDate.of(1990, 1, 15),
                "123456789"
        );
        return customerService.createCustomer(customerRequest).id();
    }

    @Test
    @DisplayName("Should deposit to account")
    void shouldDepositToAccount() {
        DepositRequest request = new DepositRequest(
                accountNumber1,
                new BigDecimal("500.00"),
                "Test deposit"
        );

        TransactionResponse response = transactionService.deposit(request);

        assertNotNull(response.transactionReference());
        assertEquals(new BigDecimal("500.00"), response.amount());
        assertEquals(new BigDecimal("1500.00"), response.balanceAfter());

        var balance = accountService.getBalance(accountNumber1);
        assertEquals(new BigDecimal("1500.00"), balance.currentBalance());
    }

    @Test
    @DisplayName("Should withdraw from account with sufficient balance")
    void shouldWithdrawFromAccount() {
        WithdrawalRequest request = new WithdrawalRequest(
                accountNumber1,
                new BigDecimal("200.00"),
                "Test withdrawal"
        );

        TransactionResponse response = transactionService.withdraw(request);

        assertNotNull(response.transactionReference());
        assertEquals(new BigDecimal("200.00"), response.amount());
        assertEquals(new BigDecimal("800.00"), response.balanceAfter());
    }

    @Test
    @DisplayName("Should reject withdrawal with insufficient balance")
    void shouldRejectWithdrawalInsufficientBalance() {
        WithdrawalRequest request = new WithdrawalRequest(
                accountNumber1,
                new BigDecimal("5000.00"),
                "Test withdrawal"
        );

        assertThrows(InsufficientFundsException.class, () ->
                transactionService.withdraw(request));
    }

    @Test
    @DisplayName("Should transfer between accounts")
    void shouldTransferBetweenAccounts() {
        TransferRequest request = new TransferRequest(
                accountNumber1,
                accountNumber2,
                new BigDecimal("300.00"),
                "Test transfer"
        );

        TransactionResponse response = transactionService.transfer(request);

        assertNotNull(response.transactionReference());

        var balance1 = accountService.getBalance(accountNumber1);
        var balance2 = accountService.getBalance(accountNumber2);

        assertEquals(new BigDecimal("700.00"), balance1.currentBalance());
        assertEquals(new BigDecimal("800.00"), balance2.currentBalance());
    }

    @Test
    @DisplayName("Should reject transfer to frozen account")
    void shouldRejectTransferToFrozenAccount() {
        // Freeze destination account
        var account = accountService.getAccountByNumber(accountNumber2);
        accountService.updateAccountStatus(account.id(),
                new UpdateAccountStatusRequest(Account.AccountStatus.FROZEN, "Test"));

        TransferRequest request = new TransferRequest(
                accountNumber1,
                accountNumber2,
                new BigDecimal("100.00"),
                "Test transfer"
        );

        assertThrows(Exception.class, () -> transactionService.transfer(request));
    }

    @Test
    @DisplayName("Should return transaction history with pagination")
    void shouldReturnTransactionHistory() {
        transactionService.deposit(new DepositRequest(
                accountNumber1, new BigDecimal("100.00"), null));
        transactionService.deposit(new DepositRequest(
                accountNumber1, new BigDecimal("200.00"), null));

        var history = transactionService.getTransactionHistory(accountNumber1, 0, 10);

        assertTrue(history.totalElements() >= 2);
    }

    @Test
    @DisplayName("Should throw when depositing to non-existent account")
    void shouldThrowWhenDepositingToNonExistentAccount() {
        DepositRequest request = new DepositRequest("BA999999", new BigDecimal("100.00"), "Test");

        assertThrows(Exception.class, () -> transactionService.deposit(request));
    }

    @Test
    @DisplayName("Should throw when withdrawing from non-existent account")
    void shouldThrowWhenWithdrawingFromNonExistentAccount() {
        WithdrawalRequest request = new WithdrawalRequest("BA999999", new BigDecimal("100.00"), "Test");

        assertThrows(Exception.class, () -> transactionService.withdraw(request));
    }

    @Test
    @DisplayName("Should throw when transferring from non-existent account")
    void shouldThrowWhenTransferringFromNonExistentAccount() {
        TransferRequest request = new TransferRequest("BA999999", accountNumber2, new BigDecimal("100.00"), "Test");

        assertThrows(Exception.class, () -> transactionService.transfer(request));
    }

    @Test
    @DisplayName("Should throw when transferring to non-existent account")
    void shouldThrowWhenTransferringToNonExistentAccount() {
        TransferRequest request = new TransferRequest(accountNumber1, "BA999999", new BigDecimal("100.00"), "Test");

        assertThrows(Exception.class, () -> transactionService.transfer(request));
    }

    @Test
    @DisplayName("Should throw when getting history for non-existent account")
    void shouldThrowWhenHistoryForNonExistentAccount() {
        assertThrows(Exception.class, () ->
                transactionService.getTransactionHistory("BA999999", 0, 10));
    }

    @Test
    @DisplayName("Should get transaction by reference")
    void shouldGetTransactionByReference() {
        DepositRequest request = new DepositRequest(
                accountNumber1, new BigDecimal("100.00"), "Test deposit"
        );
        TransactionResponse created = transactionService.deposit(request);

        TransactionResponse found = transactionService.getTransactionByReference(created.transactionReference());

        assertEquals(created.transactionReference(), found.transactionReference());
        assertEquals(created.amount(), found.amount());
    }

    @Test
    @DisplayName("Should throw when transaction reference not found")
    void shouldThrowWhenTransactionReferenceNotFound() {
        assertThrows(Exception.class, () ->
                transactionService.getTransactionByReference("TXN999999"));
    }

    @Test
    @DisplayName("Should throw when depositing to frozen account")
    void shouldThrowWhenDepositingToFrozenAccount() {
        var account = accountService.getAccountByNumber(accountNumber1);
        accountService.updateAccountStatus(account.id(),
                new UpdateAccountStatusRequest(Account.AccountStatus.FROZEN, "Test"));

        DepositRequest request = new DepositRequest(accountNumber1, new BigDecimal("100.00"), "Test");

        assertThrows(Exception.class, () -> transactionService.deposit(request));
    }

    @Test
    @DisplayName("Should throw when withdrawing from frozen account")
    void shouldThrowWhenWithdrawingFromFrozenAccount() {
        var account = accountService.getAccountByNumber(accountNumber1);
        accountService.updateAccountStatus(account.id(),
                new UpdateAccountStatusRequest(Account.AccountStatus.FROZEN, "Test"));

        WithdrawalRequest request = new WithdrawalRequest(accountNumber1, new BigDecimal("100.00"), "Test");

        assertThrows(Exception.class, () -> transactionService.withdraw(request));
    }

    @Test
    @DisplayName("Should throw when transferring from frozen source account")
    void shouldThrowWhenTransferringFromFrozenSourceAccount() {
        var account = accountService.getAccountByNumber(accountNumber1);
        accountService.updateAccountStatus(account.id(),
                new UpdateAccountStatusRequest(Account.AccountStatus.FROZEN, "Test"));

        TransferRequest request = new TransferRequest(accountNumber1, accountNumber2, new BigDecimal("100.00"), "Test");

        assertThrows(Exception.class, () -> transactionService.transfer(request));
    }
}
