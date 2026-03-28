package com.banking.api.service;

import com.banking.api.dto.AccountDTOs.*;
import com.banking.api.dto.CustomerDTOs.*;
import com.banking.api.entity.Account;
import com.banking.api.exception.BadRequestException;
import com.banking.api.exception.ResourceNotFoundException;
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
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private CustomerService customerService;

    private Long customerId;

    @BeforeEach
    void setUp() {
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                "John",
                "Doe",
                "john.account" + System.currentTimeMillis() + "@test.com",
                "+1234567890",
                "123 Main St",
                "New York",
                "NY",
                "10001",
                "USA",
                LocalDate.of(1990, 1, 15),
                "123456789"
        );

        CustomerResponse customer = customerService.createCustomer(customerRequest);
        customerId = customer.id();
    }

    @Test
    @DisplayName("Should create checking account")
    void shouldCreateCheckingAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                Account.AccountType.CHECKING,
                new BigDecimal("1000.00"),
                new BigDecimal("500.00")
        );

        AccountResponse response = accountService.createAccount(request);

        assertNotNull(response.id());
        assertNotNull(response.accountNumber());
        assertEquals(Account.AccountType.CHECKING, response.accountType());
        assertEquals(new BigDecimal("1000.00"), response.balance());
        assertEquals(Account.AccountStatus.ACTIVE, response.status());
    }

    @Test
    @DisplayName("Should create savings account")
    void shouldCreateSavingsAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                Account.AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                null
        );

        AccountResponse response = accountService.createAccount(request);

        assertEquals(Account.AccountType.SAVINGS, response.accountType());
        assertNotNull(response.interestRate());
    }

    @Test
    @DisplayName("Should return account balance")
    void shouldReturnAccountBalance() {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                Account.AccountType.CHECKING,
                new BigDecimal("1000.00"),
                new BigDecimal("500.00")
        );

        AccountResponse account = accountService.createAccount(request);
        BalanceResponse balance = accountService.getBalance(account.accountNumber());

        assertEquals(new BigDecimal("1500.00"), balance.availableBalance());
        assertEquals(new BigDecimal("1000.00"), balance.currentBalance());
    }

    @Test
    @DisplayName("Should freeze account")
    void shouldFreezeAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                Account.AccountType.CHECKING,
                new BigDecimal("100.00"),
                null
        );

        AccountResponse account = accountService.createAccount(request);

        UpdateAccountStatusRequest statusRequest = new UpdateAccountStatusRequest(
                Account.AccountStatus.FROZEN,
                "Suspicious activity"
        );

        AccountResponse updated = accountService.updateAccountStatus(account.id(), statusRequest);

        assertEquals(Account.AccountStatus.FROZEN, updated.status());
    }

    @Test
    @DisplayName("Should not close account with non-zero balance")
    void shouldCloseAccount() {
        CreateAccountRequest request = new CreateAccountRequest(
                customerId,
                Account.AccountType.CHECKING,
                BigDecimal.ZERO,
                null
        );

        AccountResponse account = accountService.createAccount(request);

        UpdateAccountStatusRequest statusRequest = new UpdateAccountStatusRequest(
                Account.AccountStatus.CLOSED,
                null
        );

        AccountResponse updated = accountService.updateAccountStatus(account.id(), statusRequest);
        assertEquals(Account.AccountStatus.CLOSED, updated.status());
    }

    @Test
    @DisplayName("Should list accounts by customer")
    void shouldListAccountsByCustomer() {
        accountService.createAccount(new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, null, null));
        accountService.createAccount(new CreateAccountRequest(
                customerId, Account.AccountType.SAVINGS, null, null));

        var accounts = accountService.getAccountsByCustomerId(customerId);

        assertEquals(2, accounts.size());
    }

    @Test
    @DisplayName("Should throw when account not found")
    void shouldThrowWhenAccountNotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                accountService.getAccountById(999999L));
    }

    @Test
    @DisplayName("Should throw when account not found by number")
    void shouldThrowWhenAccountNotFoundByNumber() {
        assertThrows(ResourceNotFoundException.class, () ->
                accountService.getAccountByNumber("BA999999"));
    }

    @Test
    @DisplayName("Should throw when getting balance for non-existent account")
    void shouldThrowWhenBalanceForNonExistentAccount() {
        assertThrows(ResourceNotFoundException.class, () ->
                accountService.getBalance("BA999999"));
    }

    @Test
    @DisplayName("Should return paginated accounts by customer")
    void shouldReturnPaginatedAccountsByCustomer() {
        accountService.createAccount(new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, null, null));
        accountService.createAccount(new CreateAccountRequest(
                customerId, Account.AccountType.SAVINGS, null, null));

        var response = accountService.getAccountsByCustomerIdPaged(customerId, 0, 10);

        assertNotNull(response.content());
        assertTrue(response.totalElements() >= 2);
    }

    @Test
    @DisplayName("Should throw when updating status of non-existent account")
    void shouldThrowWhenUpdatingNonExistentAccountStatus() {
        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(
                Account.AccountStatus.FROZEN, "Test"
        );

        assertThrows(ResourceNotFoundException.class, () ->
                accountService.updateAccountStatus(999999L, request));
    }

    @Test
    @DisplayName("Should throw when trying to modify closed account")
    void shouldThrowWhenModifyingClosedAccount() {
        CreateAccountRequest createRequest = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, BigDecimal.ZERO, null
        );
        AccountResponse account = accountService.createAccount(createRequest);

        // Close the account
        accountService.updateAccountStatus(account.id(),
                new UpdateAccountStatusRequest(Account.AccountStatus.CLOSED, null));

        // Try to modify again
        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(
                Account.AccountStatus.FROZEN, "Test"
        );

        assertThrows(BadRequestException.class, () ->
                accountService.updateAccountStatus(account.id(), request));
    }

    @Test
    @DisplayName("Should throw when closing account with non-zero balance")
    void shouldThrowWhenClosingAccountWithBalance() {
        CreateAccountRequest createRequest = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, new BigDecimal("100.00"), null
        );
        AccountResponse account = accountService.createAccount(createRequest);

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(
                Account.AccountStatus.CLOSED, null
        );

        assertThrows(BadRequestException.class, () ->
                accountService.updateAccountStatus(account.id(), request));
    }

    @Test
    @DisplayName("Should throw when creating account for non-existent customer")
    void shouldThrowWhenCreatingAccountForNonExistentCustomer() {
        CreateAccountRequest request = new CreateAccountRequest(
                999999L, Account.AccountType.CHECKING, null, null
        );

        assertThrows(ResourceNotFoundException.class, () ->
                accountService.createAccount(request));
    }
}
