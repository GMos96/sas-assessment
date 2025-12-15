package com.example.sas.features.customer.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("customer_history")
public class CustomerHistory {
    @Id
    private UUID id;
    private UUID customerId;
    private String firstName;
    private String lastName;
    private java.time.LocalDate birthday;
    private String email;
    private String phone;

    private String ssnEncrypted;
    private byte[] ssnEncryptedIv;
    private String ssnEncryptionKeyId;
    private String ssnHash;
    private String ssnMasked;

    private String changeType;
    private String changedBy;
    private OffsetDateTime changedAt;
    private Long version;

    public CustomerHistory() {}

    // Getters & setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

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

    public java.time.LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(java.time.LocalDate birthday) {
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

    public String getSsnEncrypted() {
        return ssnEncrypted;
    }

    public void setSsnEncrypted(String ssnEncrypted) {
        this.ssnEncrypted = ssnEncrypted;
    }

    public byte[] getSsnEncryptedIv() {
        return ssnEncryptedIv;
    }

    public void setSsnEncryptedIv(byte[] ssnEncryptedIv) {
        this.ssnEncryptedIv = ssnEncryptedIv;
    }

    public String getSsnEncryptionKeyId() {
        return ssnEncryptionKeyId;
    }

    public void setSsnEncryptionKeyId(String ssnEncryptionKeyId) {
        this.ssnEncryptionKeyId = ssnEncryptionKeyId;
    }

    public String getSsnHash() {
        return ssnHash;
    }

    public void setSsnHash(String ssnHash) {
        this.ssnHash = ssnHash;
    }

    public String getSsnMasked() {
        return ssnMasked;
    }

    public void setSsnMasked(String ssnMasked) {
        this.ssnMasked = ssnMasked;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(OffsetDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

