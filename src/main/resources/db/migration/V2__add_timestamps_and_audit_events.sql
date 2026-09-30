ALTER TABLE profiles
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN updated_at DATETIME(6) NULL;

ALTER TABLE apartment_relationships
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN updated_at DATETIME(6) NULL;

CREATE TABLE audit_events (
    id VARCHAR(255) PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255),
    actor_user_id VARCHAR(255) NOT NULL,
    details VARCHAR(1000),
    created_at DATETIME(6) NOT NULL
);
