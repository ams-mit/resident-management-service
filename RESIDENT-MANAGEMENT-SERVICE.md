# Project A — Resident Management Service

**File:** `11-RESIDENT-MANAGEMENT-SERVICE.md`  
**Service:** `resident-management-service`  
**Service ID:** `RESIDENT-MANAGEMENT`  
**Group:** Group 1 — Apartment Identity and Resident Management  
**Repository:** `ams-mit/resident-management-service`  
**Package:** `kln.ams.residentmanagement`  
**API version:** `v1`  
**Base path:** `/api/v1`

---

## 1. Purpose

This document defines the canonical target-state contract for `resident-management-service` in Project A.

The service owns the apartment resident-management domain within Group 1.

The documented Group 1 scope covers:

- resident management;
- owner management;
- tenant/resident management;
- staff profile management;
- user-domain relationships;
- profile information used by the apartment application;
- validation of resident/user relationships for other services.

The Group 1 frontend scope includes:

```text
/profile
/residents
/owners
/tenants
/staff
```

The assignment identifies the following indicative identity/resident entities:

```text
ResidentProfile
OwnerProfile
StaffProfile
```

and the wider identity domain also contains:

```text
User
Role
Permission
UserRole
AccountStatus
```

`User`, `Role`, `Permission`, authentication, account status, and User JWT issuance belong to `identity-access-service`. This service owns the resident-side profile and relationship information.

The apartment case requires an integrated system in which resident onboarding, maintenance requests, facility reservations, visitor handling, and billing workflows can validate the relevant resident/user relationship through documented service APIs.

**Source basis:** Group 1 responsibilities and the Apartment Management System case scope. Detailed endpoint paths, API IDs, response shapes, and some relationship representations below are the Project A canonical contract decisions derived from those requirements and the shared API/security standards.

---

# 2. Source-of-Truth Rule

The existing repository is **not** the source of truth.

This document defines the target state for `resident-management-service`.

Before implementation or synchronization, the AI/developer agent must:

1. inspect the complete existing repository;
2. inspect controllers, services, repositories, entities, DTOs, security configuration, migrations, tests, OpenAPI configuration, and application configuration;
3. compare the implementation with this contract;
4. remove obsolete APIs;
5. remove duplicate APIs;
6. remove conflicting APIs;
7. remove obsolete resident-management business logic;
8. modify incompatible implementation;
9. create missing functionality;
10. update database schema/migrations;
11. update authorization;
12. update tests;
13. update OpenAPI;
14. compile;
15. run automated tests;
16. verify Gateway integration;
17. verify Identity Access integration;
18. verify property/unit and occupancy dependencies;
19. verify standard errors;
20. verify Swagger/OpenAPI;
21. verify health;
22. remove temporary/obsolete artifacts;
23. leave the repository synchronized with this contract.

Do **not** preserve an old endpoint merely because it already exists.

Do **not** add compatibility aliases unless they are explicitly added to the canonical contract.

---

# 3. Responsibility Boundary

## 3.1 This service owns

| Area | Ownership |
|---|---|
| Resident profile | `resident-management-service` |
| Owner profile | `resident-management-service` |
| Tenant/resident profile representation | `resident-management-service` |
| Staff profile | `resident-management-service` |
| User-to-profile relationship | `resident-management-service` |
| Resident/owner/tenant/staff profile status | `resident-management-service` |
| Profile contact information | `resident-management-service` |
| Resident-domain relationship validation | `resident-management-service` |

## 3.2 This service does not own

| Data/function | Owning service |
|---|---|
| User account credentials | `identity-access-service` |
| Passwords/password hashes | `identity-access-service` |
| Authentication | `identity-access-service` |
| Roles | `identity-access-service` |
| Permissions | `identity-access-service` |
| User JWT issuance | `identity-access-service` |
| Building | `property-unit-service` |
| Floor | `property-unit-service` |
| Unit | `property-unit-service` |
| Unit type | `property-unit-service` |
| Unit ownership record | `property-unit-service` |
| Unit status/availability | `property-unit-service` |
| Lease | `lease-occupancy-service` |
| Occupancy | `lease-occupancy-service` |
| Occupant status/history | `lease-occupancy-service` |
| Invoice | `billing-payment-service` |
| Payment | `billing-payment-service` |
| Receipt | `billing-payment-service` |
| Utility charge | `utility-charge-service` |
| Maintenance request | `operations-service` |
| Work order | `operations-service` |
| Facility | `operations-service` |
| Facility booking | `operations-service` |
| Visitor | `community-service` |
| Announcement | `community-service` |
| Notification | `community-service` |

This service must not recreate another service's domain data merely to make a local operation easier.

---

# 4. Resident Domain Model

The canonical resident-management concepts are:

```text
Resident
Owner
Tenant / Resident
Staff
UserRelationship
ProfileStatus
```

The assignment explicitly identifies `ResidentProfile`, `OwnerProfile`, and `StaffProfile`.

The Project A contract additionally exposes separate public resources for:

```text
/residents
/owners
/tenants
/staff
```

This is a contract-level resource distinction for the frontend and API even where the underlying persistence model may share profile tables.

## 4.1 Tenant/resident representation

The source uses the terminology **Tenant / Resident** rather than defining a separate `TenantProfile` entity.

Therefore:

- the API exposes `/tenants` because tenant/resident administration is explicitly part of Group 1's frontend scope;
- the implementation may represent tenant and resident information using a shared profile model;
- the API must not create a second unrelated person record for the same user;
- the exact database decomposition is an implementation decision provided the external contract remains consistent.

---

# 5. User Relationship Model

A resident-management record may reference an Identity Access user:

```text
residentProfile.userId
ownerProfile.userId
tenantProfile.userId
staffProfile.userId
```

The `userId` identifies the account in `identity-access-service`.

This service must not copy:

```text
password
passwordHash
JWT
private key
role definition
permission definition
```

into its own database.

The profile service may cache/display selected identity fields when required by the API, but Identity Access remains the source of truth for account identity, account status, and roles.

---

# 6. Technology and Repository Contract

## 6.1 Technology

- Java 21
- Spring Boot 4.1.1
- Maven
- Spring Web
- Spring Validation
- Spring Security
- OpenAPI/Swagger
- MySQL
- Automated tests
- Docker

## 6.2 Maven group/package

```text
groupId:
kln.ams

base package:
kln.ams.residentmanagement
```

Recommended package structure:

```text
kln.ams.residentmanagement
├── config
├── controller
├── dto
│   ├── resident
│   ├── owner
│   ├── tenant
│   ├── staff
│   └── internal
├── entity
├── exception
├── repository
├── security
├── service
└── util
```

The exact class decomposition may vary, but the API and ownership boundaries may not.

---

# 7. Database

Database name:

```text
resident_management_db
```

Only this service owns and modifies this database.

Other services must never query it directly.

## 7.1 Core profile data

The implementation must support the profile information required by the apartment case and Group 1 scope.

A profile may contain:

```text
id
user_id
first_name
last_name
email
phone
address/contact information where applicable
profile_type
status
created_at
updated_at
```

The exact field set may be refined during implementation, but:

- `user_id` must be traceable to Identity Access;
- sensitive account credentials remain outside this database;
- apartment/unit identifiers must reference another service's data rather than becoming a duplicate unit database.

---

# 8. Profile Status

Profile status is distinct from Identity Access account status.

Identity Access owns account status.

Resident Management owns profile status.

Recommended canonical profile statuses:

```text
ACTIVE
INACTIVE
ARCHIVED
```

A profile may be inactive while the underlying identity account remains active.

Conversely, an Identity Access account may be inactive while its historical resident profile remains stored.

Profile status must not be treated as a replacement for account authentication status.

---

# 9. Canonical Roles

The project-wide role vocabulary is:

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
OWNER
TENANT_RESIDENT
FINANCE_OFFICER
MAINTENANCE_COORDINATOR
TECHNICIAN
SERVICE_STAFF
SECURITY_OFFICER
```

This service uses those roles for endpoint authorization.

It must not introduce legacy values such as:

```text
ADMIN
SYSTEM_ADMIN
MANAGER
PROPERTY_MANAGER
TENANT
RESIDENT
OWN
```

into new contracts.

Where an existing repository contains legacy role values, they must be migrated/normalized or removed during synchronization.

---

# 10. Authentication

All protected public APIs use the Project A JWT/Gateway architecture:

```text
Frontend
   ↓ User JWT
API Gateway
   ↓ validates User JWT
   ↓ creates Gateway JWT
resident-management-service
```

Internal APIs use:

```text
Calling Service
   ↓ Service JWT
API Gateway
   ↓ validates Service JWT
   ↓ creates Gateway JWT
resident-management-service
```

The service must verify the Gateway JWT according to the shared security standard.

It must not independently accept arbitrary frontend tokens that bypass the Gateway.

---

# 11. Authorization Model

Authorization is based on:

1. authenticated user identity;
2. canonical role;
3. profile ownership or administrative scope where required;
4. domain relationship where required.

Examples:

- A tenant/resident may view their own profile.
- An owner may view their own owner profile.
- An apartment manager may manage resident/owner/tenant/staff profiles.
- A system administrator may perform identity/profile administration permitted by the project.
- Other services may validate relationships through internal APIs.

A role alone does not prove:

```text
user → owns → unit
user → occupies → unit
user → leases → unit
```

Those relationships must be validated using the appropriate property/lease/occupancy APIs.

---

# 12. Public API Inventory

The canonical public provider endpoint set is:

| API ID | Method | Endpoint | Purpose |
|---|---|---|---|
| `RES-001` | GET | `/api/v1/residents` | List resident profiles |
| `RES-002` | POST | `/api/v1/residents` | Create resident profile |
| `RES-003` | GET | `/api/v1/residents/{residentId}` | Get resident profile |
| `RES-004` | PATCH | `/api/v1/residents/{residentId}` | Update resident profile |
| `OWN-001` | GET | `/api/v1/owners` | List owner profiles |
| `OWN-002` | POST | `/api/v1/owners` | Create owner profile |
| `OWN-003` | GET | `/api/v1/owners/{ownerId}` | Get owner profile |
| `TEN-001` | GET | `/api/v1/tenants` | List tenant profiles |
| `TEN-002` | POST | `/api/v1/tenants` | Create tenant profile |
| `TEN-003` | GET | `/api/v1/tenants/{tenantId}` | Get tenant profile |
| `STF-001` | GET | `/api/v1/staff` | List staff profiles |
| `STF-002` | POST | `/api/v1/staff` | Create staff profile |
| `RES-INT-001` | GET | `/api/v1/internal/residents/{residentId}/validate` | Validate resident profile |
| `RES-INT-002` | GET | `/api/v1/internal/users/{userId}/relationships` | Validate/list user-domain relationships |

**Total canonical provider endpoints: 14.**

The exact endpoint paths and IDs above are the Project A canonical contract. The assignment source explicitly supports resident/owner/tenant/staff administration and documented cross-service relationship validation, while the exact 14-endpoint decomposition is a project contract decision.

---

# 13. RES-001 — List Residents

```http
GET /api/v1/residents
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

### Query parameters

```text
page
size
status
search
userId
```

Recommended:

```text
page=0
size=20
```

Maximum recommended page size:

```text
100
```

### Purpose

Retrieve resident profiles for authorized resident-management views.

### Success

```http
200 OK
```

Example:

```json
{
  "success": true,
  "message": "Residents retrieved successfully",
  "data": {
    "items": [
      {
        "id": "uuid",
        "userId": "uuid",
        "firstName": "John",
        "lastName": "Perera",
        "email": "john@example.com",
        "phone": "0771234567",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "...",
  "requestId": "..."
}
```

### Errors

```text
400 VALIDATION_ERROR
401 INVALID_TOKEN
403 PERMISSION_DENIED
```

---

# 14. RES-002 — Create Resident

```http
POST /api/v1/residents
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

### Request

```json
{
  "userId": "uuid",
  "firstName": "John",
  "lastName": "Perera",
  "email": "john@example.com",
  "phone": "0771234567"
}
```

### Validation

- `userId` required;
- referenced user must exist in Identity Access;
- profile must not already exist for the same user where the profile model requires uniqueness;
- first name required;
- last name required;
- email must be valid;
- phone must satisfy configured validation;
- status defaults to `ACTIVE` unless the contract specifies otherwise.

### Cross-service dependency

```text
identity-access-service
```

Identity Access must validate the user before the profile is created.

### Success

```http
201 Created
```

### Errors

```text
400 VALIDATION_ERROR
401 INVALID_TOKEN
403 PERMISSION_DENIED
404 USER_NOT_FOUND
409 RESIDENT_ALREADY_EXISTS
503 DEPENDENCY_UNAVAILABLE
```

The service must not create a profile using a fabricated/nonexistent `userId`.

---

# 15. RES-003 — Get Resident

```http
GET /api/v1/residents/{residentId}
```

### Authentication

Required.

### Allowed access

- `SYSTEM_ADMINISTRATOR`
- `APARTMENT_MANAGER`
- the authenticated user when the resident profile belongs to that user, subject to the implementation's ownership check.

### Purpose

Retrieve a resident profile.

### Success

```http
200 OK
```

### Errors

```text
401 INVALID_TOKEN
403 PERMISSION_DENIED
404 RESIDENT_NOT_FOUND
```

---

# 16. RES-004 — Update Resident

```http
PATCH /api/v1/residents/{residentId}
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

A resident may update only explicitly self-service fields if the implementation exposes such a capability; unrestricted administrative fields must remain protected.

### Request

Example:

```json
{
  "firstName": "John",
  "lastName": "Perera",
  "email": "john.new@example.com",
  "phone": "0771234567"
}
```

### Rules

- `residentId` cannot be changed;
- `userId` cannot be silently reassigned through a normal profile update;
- account credentials are not updated here;
- role changes are handled by Identity Access;
- unit ownership/occupancy is not changed here;
- profile status is not changed through arbitrary fields.

### Success

```http
200 OK
```

### Errors

```text
400 VALIDATION_ERROR
403 PERMISSION_DENIED
404 RESIDENT_NOT_FOUND
409 PROFILE_CONFLICT
```

---

# 17. OWN-001 — List Owners

```http
GET /api/v1/owners
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

### Query parameters

```text
page
size
status
search
userId
```

### Purpose

List owner profiles.

This endpoint returns owner profile information only.

It must not pretend that the profile itself proves ownership of a specific apartment unit.

### Success

```http
200 OK
```

### Errors

```text
400 VALIDATION_ERROR
401 INVALID_TOKEN
403 PERMISSION_DENIED
```

---

# 18. OWN-002 — Create Owner

```http
POST /api/v1/owners
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

### Request

```json
{
  "userId": "uuid",
  "firstName": "John",
  "lastName": "Perera",
  "email": "john@example.com",
  "phone": "0771234567"
}
```

### Validation

- user must exist in Identity Access;
- owner profile must not already exist for the same user when uniqueness applies;
- required profile fields must be valid.

### Important ownership boundary

Creating an owner profile does **not** create a unit ownership record.

Unit ownership belongs to:

```text
property-unit-service
```

### Success

```http
201 Created
```

### Errors

```text
400 VALIDATION_ERROR
403 PERMISSION_DENIED
404 USER_NOT_FOUND
409 OWNER_ALREADY_EXISTS
503 DEPENDENCY_UNAVAILABLE
```

---

# 19. OWN-003 — Get Owner

```http
GET /api/v1/owners/{ownerId}
```

### Authentication

Required.

### Allowed access

- `SYSTEM_ADMINISTRATOR`
- `APARTMENT_MANAGER`
- the authenticated owner for their own profile.

### Success

```http
200 OK
```

### Errors

```text
401 INVALID_TOKEN
403 PERMISSION_DENIED
404 OWNER_NOT_FOUND
```

---

# 20. TEN-001 — List Tenants

```http
GET /api/v1/tenants
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

### Query parameters

```text
page
size
status
search
userId
```

### Purpose

List tenant/resident profiles used by the apartment management application.

The API uses `/tenants` because tenant administration is explicitly part of Group 1's frontend scope.

### Success

```http
200 OK
```

### Errors

```text
400 VALIDATION_ERROR
401 INVALID_TOKEN
403 PERMISSION_DENIED
```

---

# 21. TEN-002 — Create Tenant

```http
POST /api/v1/tenants
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

### Request

```json
{
  "userId": "uuid",
  "firstName": "John",
  "lastName": "Perera",
  "email": "john@example.com",
  "phone": "0771234567"
}
```

### Validation

- user must exist in Identity Access;
- tenant profile must not already exist for the same user where uniqueness applies;
- profile fields must be valid.

### Relationship boundary

Creating a tenant profile does **not** create a lease or occupancy.

Those belong to:

```text
lease-occupancy-service
```

### Success

```http
201 Created
```

### Errors

```text
400 VALIDATION_ERROR
403 PERMISSION_DENIED
404 USER_NOT_FOUND
409 TENANT_ALREADY_EXISTS
503 DEPENDENCY_UNAVAILABLE
```

---

# 22. TEN-003 — Get Tenant

```http
GET /api/v1/tenants/{tenantId}
```

### Authentication

Required.

### Allowed access

- `SYSTEM_ADMINISTRATOR`
- `APARTMENT_MANAGER`
- authenticated user for their own tenant/resident profile.

### Success

```http
200 OK
```

### Errors

```text
401 INVALID_TOKEN
403 PERMISSION_DENIED
404 TENANT_NOT_FOUND
```

---

# 23. STF-001 — List Staff

```http
GET /api/v1/staff
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

### Query parameters

```text
page
size
status
search
userId
```

### Purpose

List management/staff profiles required by the apartment system.

### Success

```http
200 OK
```

### Errors

```text
400 VALIDATION_ERROR
401 INVALID_TOKEN
403 PERMISSION_DENIED
```

---

# 24. STF-002 — Create Staff

```http
POST /api/v1/staff
```

### Authentication

Required.

### Allowed roles

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

### Request

```json
{
  "userId": "uuid",
  "firstName": "Jane",
  "lastName": "Perera",
  "email": "jane@example.com",
  "phone": "0771234567"
}
```

### Validation

- user must exist in Identity Access;
- staff profile must not already exist for the same user where uniqueness applies;
- profile fields must be valid.

### Important boundary

Creating a staff profile does not assign:

```text
role
permission
maintenance responsibility
security responsibility
finance responsibility
```

Role assignment belongs to Identity Access.

Domain responsibility is enforced by the owning service.

### Success

```http
201 Created
```

### Errors

```text
400 VALIDATION_ERROR
403 PERMISSION_DENIED
404 USER_NOT_FOUND
409 STAFF_ALREADY_EXISTS
503 DEPENDENCY_UNAVAILABLE
```

---

# 25. RES-INT-001 — Validate Resident

```http
GET /api/v1/internal/residents/{residentId}/validate
```

### Authentication

Required.

### Authentication type

```text
Service JWT
```

The request must arrive through the API Gateway and be represented to this service as a Gateway JWT.

### Allowed callers

Registered Project A backend services that have a documented need to validate resident identity/profile state.

Expected consumers include:

```text
property-unit-service
lease-occupancy-service
billing-payment-service
utility-charge-service
operations-service
community-service
```

The exact consuming endpoint should be documented in the cross-service registry.

### Purpose

Validate that a resident profile exists and is active.

### Success

```http
200 OK
```

Example:

```json
{
  "success": true,
  "message": "Resident validation successful",
  "data": {
    "residentId": "uuid",
    "userId": "uuid",
    "exists": true,
    "active": true
  },
  "timestamp": "...",
  "requestId": "..."
}
```

### Important boundary

This endpoint does **not** validate:

```text
unit ownership
lease
occupancy
unit availability
billing balance
maintenance eligibility
facility eligibility
visitor authorization
```

Those are validated by the owning domain services.

### Errors

```text
401 INVALID_SERVICE_TOKEN
403 CALLER_SERVICE_NOT_ALLOWED
404 RESIDENT_NOT_FOUND
503 DEPENDENCY_UNAVAILABLE
```

---

# 26. RES-INT-002 — User Relationships

```http
GET /api/v1/internal/users/{userId}/relationships
```

### Authentication

Required.

### Authentication type

```text
Service JWT
```

### Allowed callers

Registered Project A backend services with a documented requirement to validate the user's resident-domain relationships.

### Purpose

Return resident-management relationships associated with a user.

### Success

```http
200 OK
```

Example:

```json
{
  "success": true,
  "message": "User relationships retrieved successfully",
  "data": {
    "userId": "uuid",
    "relationships": [
      {
        "profileId": "uuid",
        "profileType": "RESIDENT",
        "status": "ACTIVE"
      },
      {
        "profileId": "uuid",
        "profileType": "OWNER",
        "status": "ACTIVE"
      }
    ]
  },
  "timestamp": "...",
  "requestId": "..."
}
```

### Important boundary

This endpoint describes relationships owned by Resident Management.

It must not fabricate property/occupancy facts.

For example, it should not claim:

```text
unitId = ...
occupancy = ...
leaseStatus = ...
ownershipStatus = ...
```

unless those facts are separately provided by their owning services.

### Errors

```text
400 VALIDATION_ERROR
401 INVALID_SERVICE_TOKEN
403 CALLER_SERVICE_NOT_ALLOWED
404 USER_NOT_FOUND
```

---

# 27. Public API Authentication Matrix

| API | Authentication | Allowed roles/caller |
|---|---|---|
| `RES-001` | User JWT | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` |
| `RES-002` | User JWT | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` |
| `RES-003` | User JWT | Admin/manager or own profile |
| `RES-004` | User JWT | Admin/manager; restricted self-service if explicitly implemented |
| `OWN-001` | User JWT | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` |
| `OWN-002` | User JWT | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` |
| `OWN-003` | User JWT | Admin/manager or own profile |
| `TEN-001` | User JWT | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` |
| `TEN-002` | User JWT | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` |
| `TEN-003` | User JWT | Admin/manager or own profile |
| `STF-001` | User JWT | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` |
| `STF-002` | User JWT | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` |
| `RES-INT-001` | Service JWT → Gateway JWT | Registered allowed service |
| `RES-INT-002` | Service JWT → Gateway JWT | Registered allowed service |

---

# 28. Cross-Service Dependencies

Resident Management depends primarily on:

```text
identity-access-service
```

for:

- user existence;
- account identity;
- role/account context when required.

The service also participates in cross-service workflows involving:

```text
property-unit-service
lease-occupancy-service
```

but it must not directly query their databases.

## 28.1 Resident onboarding

The documented workflow requires:

```text
Identity/resident account is created
        ↓
unit and occupancy are validated
        ↓
authorized access becomes available
```

The resident profile service must coordinate through documented APIs.

It must not create unit or occupancy records locally.

---

# 29. Property and Occupancy Boundary

The resident-management service may store a `userId` and profile information.

It must not treat a local field such as:

```text
unitId
```

as authoritative ownership/occupancy data.

For apartment relationship validation:

```text
Resident Management
        ↓
Property / Lease / Occupancy APIs
```

must be used according to the workflow.

### Example

A resident submits a maintenance request for Unit A.

The operation should conceptually validate:

```text
1. User identity
2. Resident profile/relationship
3. Unit existence/status
4. Occupancy or valid apartment relationship
```

The relevant facts come from their owning services.

---

# 30. Billing and Financial Boundary

Resident Management must not own:

```text
Invoice
InvoiceLine
Payment
Receipt
Balance
Arrears
ChargeRule
UtilityCharge
```

A billing service may use Resident Management to identify a user/profile, then use property/occupancy services to validate the eligible unit/occupancy.

The resident service must not create local financial records.

---

# 31. Operations and Community Boundary

Resident Management must not own:

```text
MaintenanceRequest
WorkOrder
Assignment
Facility
Booking
Visitor
Announcement
Notification
```

Operations and Community services consume resident validation through documented APIs.

The apartment case specifically requires maintenance and visitor workflows to validate the relevant resident/unit relationship.

Resident Management supplies the resident-side validation.

Property/occupancy services supply unit/occupancy validation.

---

# 32. Dependency Failure Behavior

If Identity Access is unavailable while creating a profile requiring user validation:

```http
503 Service Unavailable
```

must be returned.

Example:

```json
{
  "success": false,
  "message": "Identity service is temporarily unavailable",
  "error": {
    "code": "DEPENDENCY_UNAVAILABLE",
    "details": {
      "service": "identity-access-service"
    }
  },
  "timestamp": "...",
  "requestId": "..."
}
```

Do not create a profile with an unverified user.

Do not return:

```json
{
  "success": true,
  "data": {}
}
```

to conceal a dependency failure.

For synchronous calls, the project standard recommends:

```text
Connection timeout: 2 seconds
Read timeout:       5 seconds
```

The actual configured values must be documented if changed.

---

# 33. Relationship Validation Rules

A profile can reference a user only when the user exists in Identity Access.

A user may have more than one resident-domain profile representation only when the domain model explicitly supports it.

The implementation must prevent accidental duplicate people caused by:

```text
same user → multiple duplicate profiles
```

where uniqueness is required.

The service must not use email alone as the authoritative cross-service identity.

`userId` is the stable identity reference.

---

# 34. Profile Ownership Rules

A profile read/write request must distinguish:

```text
administrative access
self-service access
service-to-service validation
```

### Administrative

```text
SYSTEM_ADMINISTRATOR
APARTMENT_MANAGER
```

may manage profiles according to the endpoint contract.

### Self-service

The authenticated user may retrieve their own profile where supported.

The service must derive the authenticated user identity from the verified JWT:

```text
sub = userId
```

It must not accept an arbitrary `userId` from the request body as proof of identity.

### Service-to-service

Internal callers must use Service JWT authentication and caller authorization.

---

# 35. Request/Response Standard

All endpoints use the Project A standard response envelope.

## Success

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {},
  "timestamp": "2026-09-30T12:00:00Z",
  "requestId": "uuid"
}
```

## Error

```json
{
  "success": false,
  "message": "Unable to process request",
  "error": {
    "code": "ERROR_CODE",
    "details": null
  },
  "timestamp": "2026-09-30T12:00:00Z",
  "requestId": "uuid"
}
```

## Validation

```json
{
  "success": false,
  "message": "Request validation failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": {
      "firstName": "must not be blank",
      "email": "must be a valid email address"
    }
  },
  "timestamp": "2026-09-30T12:00:00Z",
  "requestId": "uuid"
}
```

---

# 36. Common Error Codes

| Code | Meaning |
|---|---|
| `VALIDATION_ERROR` | Request validation failed |
| `INVALID_TOKEN` | JWT invalid/missing/expired |
| `PERMISSION_DENIED` | Authenticated caller lacks required access |
| `CALLER_SERVICE_NOT_ALLOWED` | Internal caller not authorized |
| `USER_NOT_FOUND` | Referenced Identity Access user does not exist |
| `RESIDENT_NOT_FOUND` | Resident profile does not exist |
| `OWNER_NOT_FOUND` | Owner profile does not exist |
| `TENANT_NOT_FOUND` | Tenant profile does not exist |
| `STAFF_NOT_FOUND` | Staff profile does not exist |
| `RESIDENT_ALREADY_EXISTS` | Resident profile already exists |
| `OWNER_ALREADY_EXISTS` | Owner profile already exists |
| `TENANT_ALREADY_EXISTS` | Tenant profile already exists |
| `STAFF_ALREADY_EXISTS` | Staff profile already exists |
| `PROFILE_CONFLICT` | Profile state conflicts with requested operation |
| `DEPENDENCY_UNAVAILABLE` | Required service unavailable |
| `INTERNAL_SERVER_ERROR` | Unexpected server failure |

---

# 37. Validation Rules

All request DTOs must use server-side validation.

Examples:

```java
@NotBlank
private String firstName;

@NotBlank
private String lastName;

@Email
private String email;
```

Additional rules:

- UUID path parameters must be valid UUIDs;
- `userId` must be a valid UUID;
- first and last names must not be blank;
- email must satisfy configured validation;
- phone must satisfy configured validation where supplied;
- profile status must be a canonical enum;
- pagination values must be bounded;
- search input must be safely parameterized;
- unknown/unsafe fields must not be silently accepted where DTO configuration rejects them.

---

# 38. Profile Data Exposure

Responses must contain only data required for the requesting operation.

Do not expose:

```text
password
passwordHash
JWT
private key
internal service credentials
other user's unrelated profile data
```

Profile APIs must not expose another service's private domain data as if it were resident data.

---

# 39. Logging

Important operations should be logged.

Examples:

```text
RESIDENT_CREATED
RESIDENT_UPDATED
OWNER_CREATED
TENANT_CREATED
STAFF_CREATED
RESIDENT_VALIDATION_REQUEST
USER_RELATIONSHIPS_REQUEST
PROFILE_ACCESS_DENIED
```

Recommended log fields:

```text
timestamp
service
requestId
userId
operation
result
errorCode
```

Do not log:

```text
password
JWT
private keys
Authorization header
service credentials
sensitive credentials
```

---

# 40. Auditability

Administrative profile operations should be traceable.

At minimum, the logs/audit mechanism should allow determination of:

```text
who performed the operation
what profile was affected
what operation occurred
when it occurred
whether it succeeded
requestId
```

The exact persistent audit-table design is not explicitly specified by the source material and is therefore an implementation decision unless separately added to the project contract.

---

# 41. Health

The service must expose:

```http
GET /actuator/health
```

The health endpoint should verify service availability and, where configured, database availability.

It must not expose:

```text
database passwords
JWT keys
secrets
credentials
```

---

# 42. OpenAPI

Expected documentation endpoints:

```text
/swagger-ui.html
/v3/api-docs
```

Every endpoint must document:

- API ID;
- HTTP method;
- endpoint;
- purpose;
- authentication;
- allowed roles/callers;
- path parameters;
- query parameters;
- request body;
- validation;
- success response;
- error responses;
- business error codes;
- dependencies;
- dependency-failure behavior;
- examples.

The OpenAPI specification must match this contract.

Undocumented controllers must not remain in the repository.

---

# 43. Testing Requirements

## 43.1 Unit tests

At minimum test:

- resident creation;
- owner creation;
- tenant creation;
- staff creation;
- profile update;
- duplicate profile prevention;
- invalid user reference;
- profile status handling;
- user relationship validation;
- resident validation;
- authorization rules;
- validation rules;
- exception handling.

## 43.2 API/controller tests

Cover applicable:

```text
200
201
400
401
403
404
409
422
503
```

## 43.3 Security tests

Test:

- missing JWT;
- malformed JWT;
- expired JWT;
- invalid signature;
- wrong token type;
- insufficient role;
- own-profile access;
- unauthorized access to another profile;
- unauthorized service caller;
- valid internal Service JWT through Gateway.

## 43.4 Integration tests

Verify:

```text
Gateway → Resident Management
Resident Management → Identity Access
Resident Management → MySQL
```

and verify that no direct database access occurs across services.

---

# 44. Postman Requirements

The Postman collection must cover:

```text
List residents
Create resident
Get resident
Update resident

List owners
Create owner
Get owner

List tenants
Create tenant
Get tenant

List staff
Create staff

Validate resident
Get user relationships
```

Include representative:

```text
success
validation failure
unauthorized
forbidden
not found
conflict
dependency unavailable
```

cases.

---

# 45. Database Migration Requirements

Database schema changes must be versioned.

Migrations should cover the service-owned profile and relationship structures, including where applicable:

```text
resident profiles
owner profiles
tenant/resident representations
staff profiles
user_id references
profile status
timestamps
unique constraints
indexes
```

Migrations must not create tables for:

```text
users/passwords/roles/permissions owned by Identity Access
buildings/floors/units owned by Property
leases/occupancy owned by Lease
billing/payment owned by Billing
utility charges owned by Utility
maintenance/facilities owned by Operations
visitors/announcements/notifications owned by Community
```

---

# 46. API Contract Change Rules

Before changing a shared Resident Management API:

1. identify affected services;
2. update this contract;
3. update the cross-service API registry;
4. notify dependent teams;
5. update OpenAPI;
6. update Postman;
7. update automated tests;
8. update Gateway routing if necessary;
9. perform integration testing.

Do not silently change:

- endpoint path;
- HTTP method;
- authentication requirement;
- role requirement;
- request field;
- response field;
- field type;
- error code;
- status code;
- internal caller rules.

Breaking changes must follow the project's API versioning/change-management process.

---

# 47. Forbidden Patterns

The implementation must not:

- store user passwords;
- store password hashes belonging to Identity Access;
- issue User JWTs;
- define an independent JWT format;
- bypass the API Gateway for normal cross-service calls;
- directly query Identity Access database;
- directly query Property database;
- directly query Lease database;
- directly query Billing database;
- directly query Utility database;
- directly query Operations database;
- directly query Community database;
- create unit ownership records;
- create leases;
- create occupancy records;
- create invoices;
- create payments;
- create maintenance requests;
- create bookings;
- create visitors;
- create announcements;
- create notifications;
- duplicate another service's resources;
- trust a `userId` from an unverified request as authenticated identity;
- infer apartment ownership from a role;
- infer occupancy from a resident profile;
- return another service's private domain data;
- fabricate successful dependency validation;
- introduce undocumented endpoints;
- preserve obsolete/conflicting APIs merely because they exist.

---

# 48. Existing-Code Cleanup Rules

When synchronizing an existing repository:

## Remove

- duplicate resident controllers;
- duplicate owner controllers;
- duplicate tenant controllers;
- duplicate staff controllers;
- obsolete relationship endpoints;
- legacy role aliases in API authorization;
- direct database access to other services;
- old response formats;
- controller-specific error envelopes;
- obsolete DTOs/entities supporting removed APIs;
- temporary/debug endpoints;
- hard-coded secrets;
- business logic belonging to Identity, Property, Lease, Billing, Utility, Operations, or Community.

## Modify

- incompatible profile endpoints;
- incompatible user/profile relationships;
- authorization rules;
- role names;
- DTOs;
- validation;
- database schema;
- dependency clients;
- exception handling;
- OpenAPI;
- tests;
- Gateway routing configuration.

## Create

- missing canonical resident APIs;
- missing owner APIs;
- missing tenant APIs;
- missing staff APIs;
- missing internal validation APIs;
- missing Identity Access integration;
- missing dependency failure handling;
- missing security tests;
- missing database migrations;
- missing OpenAPI definitions;
- missing health configuration.

---

# 49. Cross-Team Workflow Integration

## 49.1 Resident onboarding

Required conceptual flow:

```text
User/account creation
        ↓
Resident profile creation
        ↓
Property/unit validation
        ↓
Occupancy validation
        ↓
Authorized application access
```

Resident Management owns the resident profile.

Identity Access owns the user account.

Property and Lease/Occupancy own the apartment relationship.

No service should create another service's record directly.

---

## 49.2 Maintenance request

The case requires:

```text
Authorized resident submits request for a valid unit
        ↓
Operations service creates request
```

Resident Management supplies resident/profile validation.

Property/Lease/Occupancy supply unit/apartment relationship validation.

Operations owns the maintenance request.

---

## 49.3 Facility reservation

The case requires:

```text
Resident relationship validated
        ↓
Booking rules checked
        ↓
Reservation created
```

Resident Management validates the resident/profile relationship.

Property/Occupancy validates the relevant apartment relationship.

Operations owns the facility booking.

---

## 49.4 Visitor handling

The case requires:

```text
Resident and unit validated
        ↓
Visitor entry recorded
        ↓
Authorized security/management users view relevant information
```

Resident Management provides resident-side validation.

Property/Occupancy provides unit-side validation.

Community owns the Visitor record.

---

# 50. Internal Caller Authorization

Internal requests must follow:

```text
Calling Service
    ↓ Service JWT
API Gateway
    ↓ validates service identity
    ↓ creates Gateway JWT
Resident Management
    ↓ verifies Gateway JWT
    ↓ checks allowed calling service
    ↓ performs requested validation
```

A valid Gateway JWT alone is not enough if the calling service is not authorized for the endpoint.

For example:

```text
RES-INT-001
```

must have an explicit allowed-caller policy.

Do not authorize internal callers by arbitrary user roles.

---

# 51. Relationship Response Boundary

The service must return only relationships that it actually owns.

Example:

```json
{
  "userId": "uuid",
  "relationships": [
    {
      "profileId": "uuid",
      "profileType": "RESIDENT",
      "status": "ACTIVE"
    }
  ]
}
```

It must not manufacture:

```json
{
  "unitId": "uuid",
  "leaseStatus": "ACTIVE",
  "occupancyStatus": "OCCUPIED"
}
```

unless those facts are provided by their owning services through a separate documented contract.

This prevents the resident service from becoming a hidden shared domain database.

---

# 52. Gateway Routing

The Gateway must route:

```text
/api/v1/residents/**
/api/v1/owners/**
/api/v1/tenants/**
/api/v1/staff/**
/api/v1/internal/residents/**
/api/v1/internal/users/**
```

to `resident-management-service` according to the shared Gateway contract.

The Gateway handles common ingress concerns.

Resident Management handles:

- profile business rules;
- relationship data it owns;
- profile authorization;
- profile validation.

---

# 53. Profile and Identity Separation

The canonical separation is:

```text
                 identity-access-service
                 ┌────────────────────────┐
                 │ User                   │
                 │ Role                   │
                 │ Permission             │
                 │ AccountStatus          │
                 │ Authentication         │
                 └───────────┬────────────┘
                             │ userId
                             ▼
              resident-management-service
                 ┌────────────────────────┐
                 │ ResidentProfile        │
                 │ OwnerProfile           │
                 │ Tenant/Resident data   │
                 │ StaffProfile           │
                 │ User relationships     │
                 └────────────────────────┘
```

The two services must not merge their databases.

---

# 54. Example Resident Response

```json
{
  "success": true,
  "message": "Resident retrieved successfully",
  "data": {
    "id": "0c0d9e18-9e77-4c9e-8b8d-5d9bb5eaa111",
    "userId": "7c5c9f4d-4df2-4d8e-9c7f-4d7e5e7a4c11",
    "firstName": "John",
    "lastName": "Perera",
    "email": "john@example.com",
    "phone": "0771234567",
    "status": "ACTIVE"
  },
  "timestamp": "2026-09-30T12:00:00Z",
  "requestId": "b7e9c8e1-..."
}
```

This response does not claim ownership, tenancy, or occupancy.

Those are separate domain facts.

---

# 55. Example Cross-Service Validation

A service needing resident validation should use:

```http
GET /api/v1/internal/residents/{residentId}/validate
```

Example:

```text
Operations Service
       |
       | residentId
       v
API Gateway
       |
       v
Resident Management
       |
       | exists=true
       | active=true
       v
Operations Service
```

If the request also requires unit validation:

```text
Resident validation
       ↓
Property/Lease/Occupancy validation
       ↓
Operations business rule
```

Each service owns its own fact.

---

# 56. Performance and Reliability

The assignment requires reasonable prototype performance and reliable dependency behavior.

Resident Management should:

- use bounded database queries;
- paginate collection endpoints;
- index `user_id` and other frequently queried identifiers;
- avoid unbounded list queries;
- use bounded downstream timeouts;
- fail clearly when dependencies are unavailable;
- avoid corrupting local data after dependency failure.

No performance target beyond the project's general prototype requirement is explicitly specified in the source material; teams should record observations during testing.

---

# 57. Final Implementation Checklist

## Repository

- [ ] Repository is `resident-management-service`
- [ ] Package is `kln.ams.residentmanagement`
- [ ] Java 21
- [ ] Spring Boot 4.1.1
- [ ] Maven
- [ ] MySQL
- [ ] `resident_management_db`
- [ ] Own database only
- [ ] No direct cross-service DB access

## Domain

- [ ] Resident profile implemented
- [ ] Owner profile implemented
- [ ] Tenant/resident API implemented
- [ ] Staff profile implemented
- [ ] User relationship implemented
- [ ] Profile status implemented
- [ ] `userId` references Identity Access
- [ ] No password storage
- [ ] No JWT storage

## APIs

- [ ] `RES-001`
- [ ] `RES-002`
- [ ] `RES-003`
- [ ] `RES-004`
- [ ] `OWN-001`
- [ ] `OWN-002`
- [ ] `OWN-003`
- [ ] `TEN-001`
- [ ] `TEN-002`
- [ ] `TEN-003`
- [ ] `STF-001`
- [ ] `STF-002`
- [ ] `RES-INT-001`
- [ ] `RES-INT-002`

## Security

- [ ] Gateway JWT validation
- [ ] User role authorization
- [ ] Self-profile authorization where supported
- [ ] Service caller authorization
- [ ] No apartment relationship claims trusted from JWT
- [ ] No secrets in source/logs

## Dependencies

- [ ] Identity Access user validation
- [ ] Dependency timeout configured
- [ ] Dependency failure returns `503`
- [ ] No fabricated validation success
- [ ] Property/occupancy relationships obtained through documented APIs

## API standard

- [ ] `/api/v1`
- [ ] REST naming
- [ ] JSON camelCase
- [ ] UUIDs
- [ ] ISO-8601 timestamps
- [ ] Standard response envelope
- [ ] Standard error envelope
- [ ] `X-Request-ID`
- [ ] Global exception handling
- [ ] OpenAPI
- [ ] Swagger
- [ ] Health endpoint

## Testing

- [ ] Unit tests
- [ ] Controller tests
- [ ] Security tests
- [ ] Integration tests
- [ ] Postman collection
- [ ] Negative cases
- [ ] Dependency failure cases
- [ ] Cross-service validation tests

## Operations

- [ ] `/actuator/health`
- [ ] Structured logs
- [ ] No credentials in logs
- [ ] Docker build works
- [ ] Migrations work
- [ ] Clean repository state

---

# 58. Canonical Service Boundary

```text
                    ┌─────────────────────────────────┐
                    │ resident-management-service     │
                    │                                 │
                    │ ResidentProfile                 │
                    │ OwnerProfile                    │
                    │ Tenant/Resident profile data    │
                    │ StaffProfile                    │
                    │ User relationships              │
                    │ Profile status                  │
                    │ Resident validation             │
                    └───────────────┬─────────────────┘
                                    │
                         documented APIs
                                    │
          ┌─────────────────────────┼──────────────────────────┐
          │                         │                          │
          ▼                         ▼                          ▼
 Identity Access             Property / Occupancy        Other services
 user validation             unit/lease validation       resident validation
```

---

# 59. Canonical Cross-Service Principle

The service must follow this rule:

```text
Own the profile fact.
Validate external facts through their owner.
Never duplicate another service's source of truth.
```

Examples:

```text
Resident profile
→ Resident Management

User account
→ Identity Access

Unit
→ Property Unit

Lease / Occupancy
→ Lease Occupancy

Invoice / Payment
→ Billing Payment

Utility Charge
→ Utility Charge

Maintenance / Booking
→ Operations

Visitor / Announcement / Notification
→ Community
```

---

# 60. Final Contract Statement

`resident-management-service` is the Project A owner of **resident, owner, tenant/resident, and staff profile information and the resident-side user relationships required by the apartment application**.

It integrates with `identity-access-service` for user identity validation and with property/lease/occupancy services for apartment relationships.

It must not become a duplicate identity database, apartment database, lease database, billing database, operations database, or community database.

The canonical implementation target is this document together with:

```text
00-PROJECT-A-CONTRACT-DECISIONS.md
01-PROJECT-A-GLOBAL-API-STANDARD.md
02-PROJECT-A-JWT-SECURITY-STANDARD.md
03-PROJECT-A-CROSS-SERVICE-API-REGISTRY.md
10-IDENTITY-ACCESS-SERVICE.md
```

Where an existing repository conflicts with this target contract, the repository must be changed to conform to the canonical contract rather than weakening the contract to preserve obsolete implementation.
