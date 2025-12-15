-- Flyway V2: create normalized history tables for customers and addresses

CREATE TABLE IF NOT EXISTS customer_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    birthday DATE,
    email VARCHAR(255),
    phone VARCHAR(50),

    ssn_encrypted TEXT,
    ssn_encrypted_iv BYTEA,
    ssn_encryption_key_id VARCHAR(128),
    ssn_hash VARCHAR(128),
    ssn_masked VARCHAR(32),

    change_type VARCHAR(20) NOT NULL,
    changed_by VARCHAR(200),
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_customer_history_customer_id ON customer_history (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_history_changed_at ON customer_history (customer_id, changed_at DESC);

CREATE TABLE IF NOT EXISTS address_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address_id UUID,
    customer_id UUID,
    type VARCHAR(50),
    street VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(30),
    country VARCHAR(100),
    change_type VARCHAR(20) NOT NULL,
    changed_by VARCHAR(200),
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_address_history_customer_id ON address_history (customer_id);

