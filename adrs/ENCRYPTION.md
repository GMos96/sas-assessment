# ADR: Encryption Standards for Sensitive Data

## Status
Accepted

## Context
The SAS Assessment application handles sensitive personally identifiable information (PII), specifically Social Security Numbers (SSNs), in a banking context. These data require protection both at rest and in transit to meet regulatory requirements (e.g., PCI-DSS, SOX) and ensure customer trust.

## Decision
We have adopted **AES-256-GCM encryption combined with HMAC-SHA256** for protecting SSNs and other sensitive data:

1. **Encryption Algorithm: AES-256-GCM**
   - Algorithm: Advanced Encryption Standard with 256-bit key
   - Mode: Galois/Counter Mode (GCM)
   - Padding: None (GCM handles authentication)
   - IV Length: 12 bytes (96 bits) - randomly generated per encryption
   - Auth Tag Length: 128 bits - provides authenticated encryption
   - Implementation: `AesGcmEncryptionService` in `com.example.sas.common.security.service`

2. **HMAC-SHA256 Authentication**
   - Purpose: Additional message authentication code
   - Algorithm: HMAC with SHA-256
   - Use Case: Integrity verification of encrypted data

3. **Key Management**
   - Key Storage: Environment variables
   - Key Format: Base64-encoded
   - Key Length: 32 bytes (256 bits) for AES
   - Key Rotation: Supported through Key ID tracking
   - Configuration: `app.encryption.aes256.key`, `app.encryption.hmac.key`, `app.encryption.key-id`

4. **Data Masking in Responses**
   - Sensitive Fields: SSNs are masked in API responses (e.g., XXX-XX-6789)
   - Utility: `MaskingUtil` for consistent masking
   - Purpose: Prevent accidental exposure of full SSNs in logs or API responses
   - Database Storage: Full encrypted SSN retained, only masked in output

5. **Encryption Result Structure**
   - Format: EncryptionResult DTO containing:
     - `ciphertextBase64`: Encrypted data in Base64
     - `ivBase64`: Initialization Vector in Base64
     - `keyId`: Identifier for key version (supports key rotation)

## Rationale

- **AES-256-GCM** is NIST-recommended for authenticated encryption and provides both confidentiality and authenticity in a single operation
- **GCM mode** automatically generates and validates authentication tags, eliminating need for separate MAC in most cases (HMAC kept for defense-in-depth)
- **Randomly-generated IV per encryption** prevents pattern analysis and meets NIST requirements
- **Base64 encoding** allows storage in traditional string columns and safe transmission over text protocols
- **Key ID field** enables seamless key rotation without re-encrypting all existing data
- **Masking in responses** provides defense-in-depth by limiting SSN exposure even if encryption is somehow bypassed

## Consequences

**Positive:**
- Strong cryptographic security meets industry standards
- Authenticated encryption prevents tampering
- Key rotation capability supports long-term security
- Masking provides additional protection layer
- Centralized encryption service enables consistent policy application

**Negative:**
- Key management complexity (environment variable setup required)
- IV storage overhead (12 bytes per encrypted value)
- Performance impact of encryption/decryption operations (typically negligible for SSNs)
- Must carefully manage key updates in production

## Implementation Details

### Encryption Process
1. Generate random 12-byte IV
2. Initialize AES-256-GCM cipher with IV
3. Encrypt plaintext to produce ciphertext and authentication tag
4. Base64-encode both ciphertext and IV
5. Return EncryptionResult with keyId for audit/rotation tracking

### Decryption Process
1. Base64-decode ciphertext and IV
2. Initialize AES-256-GCM cipher with IV
3. Decrypt and verify authentication tag
4. Return plaintext or throw EncryptionException on integrity failure

### Security Configuration
- Keys must be 32 bytes (256 bits) for AES-256
- Keys must be provided as Base64-encoded strings in configuration
- Application startup fails if keys are missing (fail-secure approach)
- Separate keys for AES encryption and HMAC authentication

### Database Integration
- Encrypted SSNs stored as TEXT in PostgreSQL
- Migration scripts: `db/migration/` (Flyway-managed)
- Audit trail: `CustomerHistory` table tracks all changes
- Search/Query: Must decrypt before comparison (performance consideration)

## Security Considerations

1. **Key Protection**
   - Never hardcode keys in source code
   - Use environment variables or secure vaults (AWS Secrets Manager, HashiCorp Vault)
   - Restrict key access through RBAC

2. **Data in Transit**
   - Use HTTPS/TLS 1.2+ for all API communications
   - Spring Security configured for HTTP Basic Auth (use with HTTPS in production)

3. **Logging**
   - Never log plaintext SSNs or encryption keys
   - Encryption exceptions logged without sensitive details

4. **IV Reuse Prevention**
   - Using SecureRandom for IV generation
   - GCM provides authentication even with random IVs

## References
- NIST SP 800-38D: Recommendation for Block Cipher Modes of Operation: Galois/Counter Mode
- RFC 5116: An Interface and Algorithms for Authenticated Encryption
- OWASP: Cryptographic Storage Cheat Sheet
- Implementation: `AesGcmEncryptionService.java`

