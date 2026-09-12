# Group 1 Repository Structure

## Backend Services

Group 1 maintains two Spring Boot microservices:

- identity-access-service
- resident-management-service

Both services follow a layered Controller / Service / Repository architecture.

## identity-access-service

Responsible for:

- User authentication
- JWT issuance and validation
- Role-Based Access Control (RBAC)
- User, role, and permission management

### Backend Modules

- config
- controller
- dto
- entity
- exception
- repository
- security
- service

## resident-management-service

Responsible for:

- Resident profiles
- Owner profiles
- Tenant profiles
- Staff profiles
- User-unit relationships
- Resident and unit validation

### Planned Backend Modules

- config
- controller
- dto
- entity
- exception
- repository
- service
- client

## Common Backend Structure

```text
src/
├── main/
│   ├── java/
│   │   └── <base-package>/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── entity/
│   │       ├── exception/
│   │       ├── repository/
│   │       ├── service/
│   │       └── client/
│   │
│   └── resources/
│
└── test/
    └── java/
