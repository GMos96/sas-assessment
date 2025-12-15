package com.example.sas.features.customer.dto;

import com.example.sas.features.customer.abstractions.UpdatableCustomer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Request object for updating a customer")
public class CustomerUpdateRequest implements UpdatableCustomer {

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "Customer's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "Customer's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Past(message = "Birthday must be in the past")
    @Schema(description = "Customer's date of birth", example = "1990-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate birthday;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Customer's email address", example = "john.doe@example.com")
    private String email;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Phone number must be valid (7-20 digits, may include +, spaces, hyphens, parentheses)")
    @Schema(description = "Customer's phone number", example = "+1-555-123-4567")
    private String phone;

    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{4}$", message = "SSN must be in format XXX-XX-XXXX")
    @Schema(description = "Customer's Social Security Number (will be encrypted at rest)", example = "123-45-6789", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ssn;

    @Valid
    @Size(max = 10, message = "Maximum 10 addresses allowed per customer")
    @Schema(description = "List of customer addresses")
    private List<AddressRequest> addresses;

    public CustomerUpdateRequest() {
    }

    // getters and setters

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSsn() {
        return ssn;
    }

    public void setSsn(String ssn) {
        this.ssn = ssn;
    }

    public List<AddressRequest> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<AddressRequest> addresses) {
        this.addresses = addresses;
    }
}

