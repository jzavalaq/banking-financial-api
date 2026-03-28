package com.banking.api.dto;

import com.banking.api.entity.Customer;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.Instant;

/**
 * DTOs for Customer operations.
 */
public class CustomerDTOs {

    public record CreateCustomerRequest(
            @NotBlank(message = "First name is required")
            @Size(min = 2, max = 50)
            String firstName,

            @NotBlank(message = "Last name is required")
            @Size(min = 2, max = 50)
            String lastName,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Phone is required")
            @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
            String phone,

            @NotBlank(message = "Address is required")
            String address,

            @NotBlank(message = "City is required")
            String city,

            @NotBlank(message = "State is required")
            String state,

            @NotBlank(message = "Zip code is required")
            String zipCode,

            @NotBlank(message = "Country is required")
            String country,

            @NotNull(message = "Date of birth is required")
            @Past(message = "Date of birth must be in the past")
            LocalDate dateOfBirth,

            @NotBlank(message = "Tax ID is required")
            @Pattern(regexp = "^[0-9]{9,12}$", message = "Invalid tax ID")
            String taxId
    ) {}

    public record UpdateCustomerRequest(
            @Size(min = 2, max = 50)
            String firstName,

            @Size(min = 2, max = 50)
            String lastName,

            @Email(message = "Invalid email format")
            String email,

            @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
            String phone,

            String address,
            String city,
            String state,
            String zipCode,
            String country
    ) {}

    public record CustomerResponse(
            Long id,
            String firstName,
            String lastName,
            String fullName,
            String email,
            String phone,
            String address,
            String city,
            String state,
            String zipCode,
            String country,
            LocalDate dateOfBirth,
            String taxId,
            Customer.KycStatus kycStatus,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CustomerSummary(
            Long id,
            String fullName,
            String email,
            String phone,
            Customer.KycStatus kycStatus
    ) {}
}
