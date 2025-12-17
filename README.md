# SAS Assessment - Customer Banking REST API

A Spring Boot REST API for managing customer records in a banking application with full audit history and encrypted SSN storage.

## Features

- ✅ **Full CRUD Operations** - Create, Read, Update customers
- ✅ **SSN Security** - AES-256-GCM encryption, HMAC hashing, masked responses
- ✅ **Audit History** - Complete history of all customer changes
- ✅ **Multiple Addresses** - Support for multiple addresses per customer
- ✅ **Input Validation** - Comprehensive validation with meaningful error messages
- ✅ **API Documentation** - Interactive Swagger UI
- ✅ **Database Migrations** - Flyway for version-controlled schema changes
- ✅ **Docker Support** - Docker Compose for easy local development
- ✅ **Comprehensive Tests** - Unit and integration tests

## Technology Stack

- **Java 21** (LTS)
- **Spring Boot 4.0.0** (latest)
- **Spring Data JDBC** for database access
- **PostgreSQL 16** database
- **Flyway** for database migrations
- **Spring Security** with HTTP Basic Auth
- **SpringDoc OpenAPI** for API documentation
- **TestContainers** for integration testing

## Prerequisites

- **Java 21** or later
- **Docker Desktop** (for PostgreSQL)
- **Gradle** (included via wrapper)

## Quick Start

### 1. Start PostgreSQL Database

```bash
docker-compose up -d postgres
```

This starts PostgreSQL on port 5432 with:
- Database: `mydatabase`
- Username: `myuser`
- Password: `secret`

### 2. Run the Application

```bash
# On Windows
gradlew.bat bootRun

# On Linux/Mac
./gradlew bootRun
```

The application will start on **http://localhost:8080**

### 3. Access API Documentation

Open your browser and navigate to:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

### 4. Authenticate

The API uses HTTP Basic Authentication with a default user:
- **Username:** `admin`
- **Password:** `admin123`

> ⚠️ **Note:** This is for development/demo only. In production, use proper authentication (JWT, OAuth2).

## API Endpoints

### Customer Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/customers` | Create a new customer |
| GET | `/api/customers/{id}` | Get customer by ID |
| PUT | `/api/customers/{id}` | Update customer |
| GET | `/api/customers/{id}/history` | Get customer change history |

### Example: Create Customer

```bash
curl -X POST http://localhost:8080/api/customers \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "birthday": "1990-01-15",
    "email": "john.doe@example.com",
    "phone": "+1-555-123-4567",
    "ssn": "123-45-6789",
    "addresses": [
      {
        "type": "HOME",
        "street": "123 Main Street",
        "city": "New York",
        "state": "NY",
        "postalCode": "10001",
        "country": "USA"
      }
    ]
  }'
```

### Example Response

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "firstName": "John",
  "lastName": "Doe",
  "birthday": "1990-01-15",
  "email": "john.doe@example.com",
  "phone": "+1-555-123-4567",
  "ssnMasked": "XXX-XX-6789",
  "createdAt": "2025-12-14T21:00:00Z",
  "updatedAt": "2025-12-14T21:00:00Z",
  "addresses": [
    {
      "id": "234e5678-e89b-12d3-a456-426614174001",
      "type": "HOME",
      "street": "123 Main Street",
      "city": "New York",
      "state": "NY",
      "postalCode": "10001",
      "country": "USA"
    }
  ]
}
```

> **Note:** SSN is always returned masked (XXX-XX-6789). The actual SSN is encrypted in the database.

## Security Features

### SSN Protection (PII Security)

The application implements multiple layers of security for Social Security Numbers:

1. **Encryption at Rest:** SSN is encrypted using AES-256-GCM before storage
2. **HMAC Hashing:** SHA-256 HMAC for duplicate detection without storing plaintext
3. **Masking:** API responses only show last 4 digits (XXX-XX-6789)
4. **Key Rotation Support:** Encryption key ID tracked for future key rotation
5. **IV Randomization:** Unique initialization vector per encryption

### Database Schema

```sql
-- SSN stored as encrypted data
ssn_encrypted TEXT,              -- Base64 encrypted SSN
ssn_encrypted_iv BYTEA,          -- Initialization vector
ssn_encryption_key_id VARCHAR,   -- Key ID for rotation
ssn_hash VARCHAR NOT NULL,       -- HMAC for duplicate detection
ssn_masked VARCHAR,              -- Masked version for display
```

### Encryption Configuration

Encryption keys are configured via environment variables or `application.properties`:

```properties
# AES-256 key (32 bytes, Base64 encoded)
app.encryption.aes256.key=${ENCRYPTION_KEY_BASE64}

# HMAC key (Base64 encoded)
app.encryption.hmac.key=${ENCRYPTION_HMAC_KEY_BASE64}

# Key identifier for rotation
app.encryption.key-id=${ENCRYPTION_KEY_ID:default}
```

> ⚠️ **Production Security:** In production:
> - Store keys in a secure vault (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault)
> - Never commit keys to version control
> - Rotate keys periodically
> - Use TLS for all connections
> - Consider implementing JWT or OAuth2 instead of Basic Auth

## Audit History

Every customer change is tracked in the `customer_history` table:

- **Snapshot approach:** Complete customer state is saved before each update
- **Change type:** CREATED, UPDATED
- **Timestamp:** When the change occurred
- **Version:** Optimistic locking version number

### Example: Get Customer History

```bash
curl -X GET http://localhost:8080/api/customers/{id}/history \
  -u admin:admin123
```

Returns an array of historical records ordered by most recent first.

## Database Schema

### Tables

- **customers** - Current customer data
- **addresses** - Customer addresses (1-to-many)
- **customer_history** - Complete audit trail
- **address_history** - (Reserved for future use)

### Migrations

Database schema is managed by Flyway. Migrations are in `src/main/resources/db/migration/`:

- `V1__create_customers_addresses_tables.sql` - Initial schema
- `V2__create_history_tables.sql` - History tracking

Flyway runs automatically on application startup.

## Testing

### Run All Tests

```bash
# On Windows
gradlew.bat test

# On Linux/Mac
./gradlew test
```

### Test Coverage

- **Unit Tests:** Mappers, DTOs, validation
- **Integration Tests:** Service layer with real database (TestContainers)

### Test Reports

After running tests, view the report:
```
build/reports/tests/test/index.html
```

> **Note:** Integration tests use TestContainers to spin up a PostgreSQL container. Ensure Docker Desktop is running.

## Configuration

### Application Properties

Main configuration in `src/main/resources/application.properties`:

```properties
# Database (overridden by Docker Compose)
spring.datasource.url=jdbc:postgresql://localhost:5432/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret

# Encryption keys (use environment variables in production)
app.encryption.aes256.key=${ENCRYPTION_KEY_BASE64}
app.encryption.hmac.key=${ENCRYPTION_HMAC_KEY_BASE64}

# Flyway migrations
spring.flyway.enabled=true

# Swagger UI
springdoc.swagger-ui.path=/swagger-ui.html
```

### Docker Compose

The `compose.yaml` includes:
- PostgreSQL 16
- Grafana LGTM stack (for observability)

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/sas/
│   │   ├── features/customer/
│   │   │   ├── entity/          # Domain models (Customer, Address)
│   │   │   ├── dto/             # API contracts (Request/Response)
│   │   │   ├── repository/      # Data access (Spring Data JDBC)
│   │   │   ├── service/         # Business logic
│   │   │   ├── web/             # REST controllers
│   │   │   ├── mapper/          # Entity ↔ DTO conversions
│   │   │   ├── util/            # Helper utilities
│   │   │   └── exceptions/      # Custom exceptions
│   │   ├── common/security/     # Encryption services
│   │   └── core/config/         # Spring configuration
│   └── resources/
│       ├── application.properties
│       └── db/migration/        # Flyway SQL scripts
└── test/
    ├── java/com/example/sas/
    │   ├── features/customer/   # Unit tests
    │   └── integration/         # Integration tests
    └── resources/
        └── application-test.properties
```

## Development

### Build

```bash
gradlew.bat build
```

### Run in IntelliJ IDEA

1. Import project as Gradle project
2. Run `SasAssessmentApplication` main class
3. Or use the Gradle panel: Tasks → application → bootRun

### Generate Encryption Keys

To generate new encryption keys for production:

```bash
# Generate AES-256 key (32 bytes)
openssl rand -base64 32

# Generate HMAC key
openssl rand -base64 32
```

## Production Considerations

Before deploying to production:

- [ ] Move encryption keys to a secrets manager
- [ ] Implement proper authentication (JWT, OAuth2)
- [ ] Enable HTTPS/TLS
- [ ] Add rate limiting
- [ ] Set up monitoring and alerts
- [ ] Configure log aggregation
- [ ] Implement key rotation procedures
- [ ] Add health check endpoints
- [ ] Configure connection pool sizing
- [ ] Enable CSRF protection (if needed)
- [ ] Review and harden security settings
- [ ] Add pagination to history endpoint
- [ ] Implement caching strategy
- [ ] Set up CI/CD pipeline
- [ ] Perform security audit
- [ ] Load testing

## Troubleshooting

### PostgreSQL Connection Failed

Ensure Docker container is running:
```bash
docker ps
```

If not running:
```bash
docker-compose up -d postgres
```

### Port 8080 Already in Use

Change the port in `application.properties`:
```properties
server.port=8081
```

### Integration Tests Failing

Integration tests require Docker Desktop to be running for TestContainers.

Check Docker status:
```bash
docker info
```

### Encryption Key Errors

Ensure encryption keys are properly configured in `application.properties` or as environment variables.

## License

This is a sample assessment project for demonstration purposes.

## Contact

For questions about this implementation, please refer to the code review documentation in `CODE_REVIEW.md`.

