package com.banking.api.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Scaffold test to verify the application context loads correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class ScaffoldTest {

    @Test
    @DisplayName("Application context should load")
    void contextLoads() {
        // If this test passes, the Spring context loaded successfully
        assertNotNull(this);
    }
}
