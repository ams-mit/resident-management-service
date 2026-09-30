CREATE TABLE profiles (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(255),
    profile_type VARCHAR(255),
    emergency_contact VARCHAR(255),
    tax_id VARCHAR(255),
    department VARCHAR(255)
);

CREATE TABLE apartment_relationships (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    relationship_type VARCHAR(255) NOT NULL,
    unit_reference VARCHAR(255) NOT NULL,
    supporting_info VARCHAR(255),
    status VARCHAR(255),
    decision_reason VARCHAR(255)
);
