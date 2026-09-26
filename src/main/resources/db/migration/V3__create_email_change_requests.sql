CREATE TABLE email_change_requests (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    new_email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_email_change_requests_token_hash ON email_change_requests(token_hash);
CREATE INDEX idx_email_change_requests_user_id ON email_change_requests(user_id);
