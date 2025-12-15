-- Flyway V1: create customers and addresses tables

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birthday DATE,
    email VARCHAR(255),
    phone VARCHAR(50),

    -- encrypted SSN storage
    ssn_encrypted TEXT,
    ssn_encrypted_iv BYTEA,
    ssn_encryption_key_id VARCHAR(128),

    -- HMAC hash for lookups and uniqueness
    ssn_hash VARCHAR(128) NOT NULL,
    ssn_masked VARCHAR(32),

    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    deleted BOOLEAN DEFAULT false,
    version BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_ssn_hash ON customers (ssn_hash);
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers (created_at);

CREATE TABLE IF NOT EXISTS addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    type VARCHAR(50),
    street VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(30),
    country VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_addresses_customer_id ON addresses (customer_id);

