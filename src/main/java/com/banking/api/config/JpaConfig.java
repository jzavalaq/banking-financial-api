package com.banking.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration for enabling auditing.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
