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
## Branch Naming Convention

The team uses the following branch naming convention:

- `feature/<short-description>` - new functionality
- `bugfix/<short-description>` - bug fixes
- `hotfix/<short-description>` - urgent fixes

Examples:

- `feature/resident-profile`
- `feature/owner-profile`
- `feature/tenant-registration`
- `bugfix/resident-validation`
- `hotfix/security-fix`

Branch names should use lowercase letters and hyphens.

## Pull Request and Merge Rules

1. Developers should create a feature or bugfix branch from `develop`.
2. Changes should be submitted through a Pull Request.
3. Pull Requests to `develop` must pass the GitHub Actions build and tests.
4. Pull Requests should be reviewed by at least one other team member.
5. The Pull Request template should be completed before merging.
6. Direct pushes to `main` should not be used.
7. Changes should reach `main` through a Pull Request after development and testing.
