package com.example.sas.features.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Response object containing address information")
public class AddressResponse {
    @Schema(description = "Unique identifier of the address", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID id;

    @Schema(description = "Address type (e.g., HOME, WORK, BILLING, SHIPPING)", example = "HOME")
    private String type;

    @Schema(description = "Street address", example = "123 Main Street")
    private String street;

    @Schema(description = "City name", example = "New York")
    private String city;

    @Schema(description = "State or province", example = "NY")
    private String state;

    @Schema(description = "Postal/ZIP code", example = "10001")
    private String postalCode;

    @Schema(description = "Country name", example = "USA")
    private String country;

    public AddressResponse() {}

    // getters/setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

