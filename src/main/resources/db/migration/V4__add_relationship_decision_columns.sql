ALTER TABLE apartment_relationships
    ADD COLUMN decided_by VARCHAR(255) NULL,
    ADD COLUMN decided_at DATETIME(6) NULL,
    MODIFY COLUMN decision_reason VARCHAR(500) NULL;
