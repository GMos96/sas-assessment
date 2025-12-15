package com.example.sas.common.security.dto;

public class EncryptionResult {
    private final String ciphertextBase64;
    private final String ivBase64;
    private final String keyId;

    public EncryptionResult(String ciphertextBase64, String ivBase64, String keyId) {
        this.ciphertextBase64 = ciphertextBase64;
        this.ivBase64 = ivBase64;
        this.keyId = keyId;
    }

    public String getCiphertextBase64() {
        return ciphertextBase64;
    }

    public String getIvBase64() {
        return ivBase64;
    }

    public String getKeyId() {
        return keyId;
    }
}

