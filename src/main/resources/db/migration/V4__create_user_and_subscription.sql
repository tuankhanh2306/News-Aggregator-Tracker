-- V4__create_user_and_subscription.sql
-- Create app_user and subscription tables

CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_user_email ON app_user(email);

CREATE TABLE subscription (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, category_id)
);

CREATE INDEX idx_subscription_user ON subscription(user_id);
CREATE INDEX idx_subscription_category ON subscription(category_id);
