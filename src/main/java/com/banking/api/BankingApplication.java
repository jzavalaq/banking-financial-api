package com.banking.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling
public class BankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }
}
