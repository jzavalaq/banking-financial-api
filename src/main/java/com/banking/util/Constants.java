package com.banking.util;

/**
 * Application-wide constants.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Account number prefix
    public static final String ACCOUNT_NUMBER_PREFIX = "BA";

    // Transaction reference prefix
    public static final String TRANSACTION_REFERENCE_PREFIX = "TXN";

    // Payment reference prefix
    public static final String PAYMENT_REFERENCE_PREFIX = "PAY";

    // Loan number prefix
    public static final String LOAN_NUMBER_PREFIX = "LN";

    // Token type
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    // Default token expiration (15 minutes in milliseconds)
    public static final long DEFAULT_ACCESS_TOKEN_EXPIRATION = 900000L;

    // Default interest rates
    public static final String DEFAULT_CHECKING_INTEREST_RATE = "0.001";
    public static final String DEFAULT_SAVINGS_INTEREST_RATE = "0.025";
    public static final String DEFAULT_MORTGAGE_INTEREST_RATE = "0.065";
    public static final String DEFAULT_PERSONAL_LOAN_INTEREST_RATE = "0.085";

    // Minimum loan amount
    public static final String MINIMUM_LOAN_AMOUNT = "1000.00";

    // Loan term bounds (in months)
    public static final int MINIMUM_LOAN_TERM_MONTHS = 6;
    public static final int MAXIMUM_LOAN_TERM_MONTHS = 360;
}
