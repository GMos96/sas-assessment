package com.example.sas.features.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request object for customer address")
public class AddressRequest {
    @NotBlank(message = "Address type is required")
    @Size(max = 50, message = "Address type must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z_]+$", message = "Address type must be uppercase (e.g., HOME, WORK, BILLING, SHIPPING)")
    @Schema(description = "Address type (e.g., HOME, WORK, BILLING, SHIPPING)", example = "HOME")
    private String type;

    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address must not exceed 200 characters")
    @Schema(description = "Street address", example = "123 Main Street")
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    @Schema(description = "City name", example = "New York")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Schema(description = "State or province", example = "NY")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 30, message = "Postal code must not exceed 30 characters")
    @Schema(description = "Postal/ZIP code", example = "10001")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    @Schema(description = "Country name", example = "USA")
    private String country;

    public AddressRequest() {}

    // getters/setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}

