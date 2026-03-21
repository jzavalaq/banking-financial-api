package com.banking.service;

import com.banking.dto.AccountDTOs.*;
import com.banking.dto.CustomerDTOs.*;
import com.banking.dto.PaymentDTOs.*;
import com.banking.entity.Account;
import com.banking.entity.Payment;
import com.banking.exception.BadRequestException;
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
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CustomerService customerService;

    private String accountNumber;

    @BeforeEach
    void setUp() {
        Long customerId = createCustomer();
        CreateAccountRequest request = new CreateAccountRequest(
                customerId, Account.AccountType.CHECKING, new BigDecimal("1000.00"), null);
        accountNumber = accountService.createAccount(request).accountNumber();
    }

    private Long createCustomer() {
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
                "John",
                "Doe",
                "john.pay" + System.currentTimeMillis() + "@test.com",
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
    @DisplayName("Should create bill payment")
    void shouldCreateBillPayment() {
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

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response.paymentReference());
        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals(Payment.PaymentStatus.COMPLETED, response.status());
    }

    @Test
    @DisplayName("Should get payment by ID")
    void shouldGetPaymentById() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber,
                Payment.PaymentType.BILL,
                new BigDecimal("50.00"),
                "Gas Company",
                "GAS123456",
                "Bill-456",
                LocalDate.now().plusDays(1),
                "Gas bill"
        );

        PaymentResponse created = paymentService.createPayment(request);
        PaymentResponse found = paymentService.getPaymentById(created.id());

        assertEquals(created.id(), found.id());
    }

    @Test
    @DisplayName("Should list payments by account")
    void shouldListPaymentsByAccount() {
        paymentService.createPayment(new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("50.00"),
                "Test1", "ACCT1", "REF1", null, null));
        paymentService.createPayment(new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("75.00"),
                "Test2", "ACCT2", "REF2", null, null));

        var payments = paymentService.getPaymentsByAccount(accountNumber, 0, 10);

        assertTrue(payments.totalElements() >= 2);
    }

    @Test
    @DisplayName("Should cancel pending payment")
    void shouldCancelPendingPayment() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber,
                Payment.PaymentType.BILL,
                new BigDecimal("100.00"),
                "Test Payee",
                "TEST123",
                "REF123",
                LocalDate.now().plusDays(7),
                "Scheduled payment"
        );

        PaymentResponse created = paymentService.createPayment(request);
        PaymentResponse cancelled = paymentService.cancelPayment(created.id());

        assertEquals(Payment.PaymentStatus.CANCELLED, cancelled.status());
    }

    @Test
    @DisplayName("Should reject cancellation of completed payment")
    void shouldRejectCancellationOfCompletedPayment() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber,
                Payment.PaymentType.BILL,
                new BigDecimal("50.00"),
                "Test Payee",
                "TEST123",
                "REF123",
                null,
                "Immediate payment"
        );

        PaymentResponse created = paymentService.createPayment(request);

        assertThrows(BadRequestException.class, () ->
                paymentService.cancelPayment(created.id()));
    }

    @Test
    @DisplayName("Should throw when creating payment for non-existent account")
    void shouldThrowWhenCreatingPaymentForNonExistentAccount() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                "BA999999", Payment.PaymentType.BILL, new BigDecimal("50.00"),
                "Test", "TEST123", "REF123", null, null
        );

        assertThrows(Exception.class, () -> paymentService.createPayment(request));
    }

    @Test
    @DisplayName("Should throw when creating payment with insufficient funds")
    void shouldThrowWhenCreatingPaymentWithInsufficientFunds() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("5000.00"),
                "Test", "TEST123", "REF123", null, null
        );

        assertThrows(Exception.class, () -> paymentService.createPayment(request));
    }

    @Test
    @DisplayName("Should throw when getting non-existent payment")
    void shouldThrowWhenGettingNonExistentPayment() {
        assertThrows(Exception.class, () -> paymentService.getPaymentById(999999L));
    }

    @Test
    @DisplayName("Should get payment by reference")
    void shouldGetPaymentByReference() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("50.00"),
                "Test", "TEST123", "REF123", null, null
        );

        PaymentResponse created = paymentService.createPayment(request);
        PaymentResponse found = paymentService.getPaymentByReference(created.paymentReference());

        assertEquals(created.id(), found.id());
        assertEquals(created.paymentReference(), found.paymentReference());
    }

    @Test
    @DisplayName("Should throw when payment reference not found")
    void shouldThrowWhenPaymentReferenceNotFound() {
        assertThrows(Exception.class, () ->
                paymentService.getPaymentByReference("PAY999999"));
    }

    @Test
    @DisplayName("Should throw when cancelling non-existent payment")
    void shouldThrowWhenCancellingNonExistentPayment() {
        assertThrows(Exception.class, () -> paymentService.cancelPayment(999999L));
    }

    @Test
    @DisplayName("Should create scheduled payment with pending status")
    void shouldCreateScheduledPaymentWithPendingStatus() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber,
                Payment.PaymentType.BILL,
                new BigDecimal("100.00"),
                "Test Payee",
                "TEST123",
                "REF123",
                LocalDate.now().plusDays(7),
                "Scheduled payment"
        );

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(Payment.PaymentStatus.PENDING, response.status());
        assertNotNull(response.scheduledDate());
    }

    @Test
    @DisplayName("Should throw when creating payment for frozen account")
    void shouldThrowWhenCreatingPaymentForFrozenAccount() {
        // Freeze the account
        var account = accountService.getAccountByNumber(accountNumber);
        accountService.updateAccountStatus(account.id(),
                new UpdateAccountStatusRequest(Account.AccountStatus.FROZEN, "Test"));

        CreatePaymentRequest request = new CreatePaymentRequest(
                accountNumber, Payment.PaymentType.BILL, new BigDecimal("50.00"),
                "Test", "TEST123", "REF123", null, null
        );

        assertThrows(Exception.class, () -> paymentService.createPayment(request));
    }
}
