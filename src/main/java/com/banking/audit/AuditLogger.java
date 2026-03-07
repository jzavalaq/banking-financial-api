package com.banking.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit Logger for tracking significant business operations.
 */
@Component
@Slf4j
public class AuditLogger {

    /**
     * Log an audit event.
     *
     * @param eventType the type of event (e.g., "LOGIN", "TRANSACTION", "ACCOUNT_CREATE")
     * @param userId    the user ID performing the action
     * @param resource  the resource being acted upon
     * @param action    the action being performed
     * @param details   additional details about the event
     */
    public void logEvent(String eventType, String userId, String resource, String action, String details) {
        String traceId = UUID.randomUUID().toString();
        log.info("AUDIT | traceId={} | timestamp={} | eventType={} | userId={} | resource={} | action={} | details={}",
                traceId, Instant.now(), eventType, userId, resource, action, details);
    }

    /**
     * Log a security-related audit event.
     *
     * @param eventType the type of security event
     * @param userId    the user ID
     * @param ipAddress the IP address of the request
     * @param details   additional details
     */
    public void logSecurityEvent(String eventType, String userId, String ipAddress, String details) {
        String traceId = UUID.randomUUID().toString();
        log.warn("SECURITY_AUDIT | traceId={} | timestamp={} | eventType={} | userId={} | ipAddress={} | details={}",
                traceId, Instant.now(), eventType, userId, ipAddress, details);
    }
}
