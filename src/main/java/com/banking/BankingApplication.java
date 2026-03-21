package com.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Banking Financial API - Main Application.
 *
 * A production-grade banking and financial services API built with Spring Boot 3.2.x.
 * Provides RESTful endpoints for customer management, account operations,
 * transactions, payments, and loan management.
 *
 * @author Banking Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableRetry
@EnableCaching
public class BankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }
}
