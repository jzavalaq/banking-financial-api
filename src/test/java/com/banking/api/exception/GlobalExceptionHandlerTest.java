package com.banking.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

    @Test
    @DisplayName("Should handle ResourceNotFoundException")
    void handleResourceNotFoundException_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Customer", "id", 123L);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertTrue(response.getBody().message().contains("Customer"));
    }

    @Test
    @DisplayName("Should handle BadRequestException")
    void handleBadRequestException_returns400() {
        BadRequestException ex = new BadRequestException("Invalid request");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Invalid request", response.getBody().message());
    }

    @Test
    @DisplayName("Should handle InsufficientFundsException")
    void handleInsufficientFundsException_returns422() {
        InsufficientFundsException ex = new InsufficientFundsException("BA123", "500.00", "100.00");

        ResponseEntity<ErrorResponse> response = handler.handleInsufficientFundsException(ex, webRequest);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().status());
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException")
    void handleDuplicateResourceException_returns409() {
        DuplicateResourceException ex = new DuplicateResourceException("Customer", "email", "test@test.com");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResourceException(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException")
    @SuppressWarnings("unchecked")
    void handleValidationExceptions_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "must be valid");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ValidationErrorResponse> response = handler.handleValidationExceptions(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Validation failed", response.getBody().message());
        assertNotNull(response.getBody().errors());
        assertTrue(response.getBody().errors().containsKey("email"));
    }

    @Test
    @DisplayName("Should handle BadCredentialsException")
    void handleBadCredentialsException_returns401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().status());
        assertEquals("Invalid credentials", response.getBody().message());
    }

    @Test
    @DisplayName("Should handle AccessDeniedException")
    void handleAccessDeniedException_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, webRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().status());
        assertEquals("Access denied", response.getBody().message());
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void handleGlobalException_returns500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("An unexpected error occurred", response.getBody().message());
    }
}
