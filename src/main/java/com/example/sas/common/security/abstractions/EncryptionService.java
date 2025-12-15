package com.example.sas.common.security.abstractions;

import com.example.sas.common.security.dto.EncryptionResult;
import com.example.sas.common.security.exception.EncryptionException;

public interface EncryptionService {
    EncryptionResult encrypt(byte[] plaintext) throws EncryptionException;
    byte[] decrypt(EncryptionResult result) throws EncryptionException;
    String hmacSha256(String input) throws EncryptionException;
}

