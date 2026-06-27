CREATE DATABASE IF NOT EXISTS queueless_ai;
USE queueless_ai;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(40) NOT NULL,
    address VARCHAR(300) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    email VARCHAR(160) NOT NULL,
    working_hours VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_organizations_type (type),
    INDEX idx_organizations_active (active)
);

CREATE TABLE IF NOT EXISTS counters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    counter_name VARCHAR(120) NOT NULL,
    counter_number INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    service_type VARCHAR(120) NOT NULL,
    CONSTRAINT fk_counters_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE CASCADE,
    INDEX idx_counters_organization (organization_id),
    INDEX idx_counters_status (status)
);

CREATE TABLE IF NOT EXISTS tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_number VARCHAR(60) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    counter_id BIGINT NOT NULL,
    booking_time TIMESTAMP NOT NULL,
    called_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL,
    estimated_wait_time INT NOT NULL,
    qr_payload VARCHAR(1000),
    qr_code_data LONGTEXT,
    CONSTRAINT fk_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tokens_counter
        FOREIGN KEY (counter_id) REFERENCES counters(id)
        ON DELETE CASCADE,
    INDEX idx_tokens_status (status),
    INDEX idx_tokens_user (user_id),
    INDEX idx_tokens_counter_status_time (counter_id, status, booking_time)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    INDEX idx_notifications_user_sent (user_id, sent_at)
);

CREATE TABLE IF NOT EXISTS queue_analytics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    date DATE NOT NULL,
    visitors INT NOT NULL,
    average_wait_time DOUBLE NOT NULL,
    completed_services INT NOT NULL,
    cancelled_tokens INT NOT NULL,
    INDEX idx_queue_analytics_date (date)
);
