-- V1__init.sql - Initial database schema for Banking Financial API

-- Users table (for authentication)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Customers table
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    tax_id VARCHAR(20) NOT NULL UNIQUE,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user_id BIGINT UNIQUE,
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Accounts table
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    interest_rate DECIMAL(5,4),
    overdraft_limit DECIMAL(19,4) DEFAULT 0,
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_reference VARCHAR(30) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL,
    related_account_id BIGINT,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    description VARCHAR(500),
    category VARCHAR(100),
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transaction_related_account FOREIGN KEY (related_account_id) REFERENCES accounts(id)
);

-- Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_reference VARCHAR(30) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL,
    payment_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    payee_name VARCHAR(100) NOT NULL,
    payee_account_number VARCHAR(50) NOT NULL,
    payee_reference VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scheduled_date DATE,
    description VARCHAR(500),
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_payment_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Loans table
CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    loan_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    account_id BIGINT,
    loan_type VARCHAR(20) NOT NULL,
    principal_amount DECIMAL(19,4) NOT NULL,
    interest_rate DECIMAL(5,4) NOT NULL,
    term_months INTEGER NOT NULL,
    outstanding_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    monthly_payment DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    application_date DATE,
    approval_date DATE,
    disbursement_date DATE,
    next_payment_date DATE,
    purpose VARCHAR(500),
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_loan_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Audit logs table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(50) NOT NULL,
    user_agent VARCHAR(500),
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
-- Users table indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- Customers table indexes
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_tax_id ON customers(tax_id);
CREATE INDEX idx_customers_kyc_status ON customers(kyc_status);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_user_id ON customers(user_id);

-- Accounts table indexes
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);

-- Transactions table indexes
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_reference ON transactions(transaction_reference);

-- Payments table indexes
CREATE INDEX idx_payments_account_id ON payments(account_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_reference ON payments(payment_reference);
CREATE INDEX idx_payments_scheduled_date ON payments(scheduled_date);

-- Loans table indexes
CREATE INDEX idx_loans_customer_id ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_loan_number ON loans(loan_number);

-- Audit logs table indexes
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
