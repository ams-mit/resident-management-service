# Project A — Resident Management Service 🏢

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Architecture](https://img.shields.io/badge/Architecture-Microservice-orange)

The **Resident Management Service** (`resident-management-service`) is an authoritative core domain microservice for the University of Kelaniya Apartment Management System (AMS Project A, Group 1 — Apartment Identity and Resident Management). It manages personal profiles, contact records, and metadata for Residents, Owners, Tenants, and Staff members, as well as providing internal validation and relationship query APIs for other Project A microservices.

This service operates as a stateless **OAuth2 Resource Server** running on port **8081** behind the shared API Gateway.

---

## 🏗️ Architecture & Boundaries

* **Authoritative Domain Scope:**
  * Manages personal profile records (`first_name`, `last_name`, `email`, `phone`, `profile_type`, `status`) for Residents, Owners, Tenants, and Staff.
  * Provides internal verification and relationship lookup endpoints for cross-service authorization and validation (`RES-INT-001`, `RES-INT-002`).
  * **Strict Boundary Exclusions:**
    * Property, building, floor, unit, and unit ownership records are strictly owned by `property-unit-service`.
    * Unit occupancy, lease agreements, and tenant assignments are strictly owned by `lease-occupancy-service`.
    * User accounts, authentication credentials, passwords, role assignments, permissions, and email change verification flows are strictly owned by `identity-access-service`.
    * Invoices, charges, payments, and receipts are strictly owned by `billing-payment-service` and `utility-charge-service`.
    * Maintenance requests, work orders, facilities, and bookings are strictly owned by `operations-service`.
    * Visitors, announcements, and notifications are strictly owned by `community-service`.
* **Stateless Authentication & Security:**
  * Validates API Gateway-issued RS256 JSON Web Tokens using the public key (`GATEWAY_JWT_PUBLIC_KEY`).
  * User endpoints (`/api/v1/**`) require signed Gateway User JWTs (`type=user`, `sub=userId`, `roles`).
  * Internal endpoints (`/api/v1/internal/**`) require signed Gateway Service JWTs (`type=service`, `sub=calling-service`) with caller allowlist authorization.
  * Public endpoints: `/actuator/health/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`.
* **Outbound Cross-Service Integration:**
  * Calls `identity-access-service` via `GET /api/v1/internal/users/{userId}/validate` (`IAM-INT-001`) with a signed Service JWT (`alg=RS256`, `typ=JWT`, `type=service`, `sub=resident-management-service`) to verify user existence and active status before profile creation.
  * Bounded timeouts: Connection timeout = 2 seconds, Read timeout = 5 seconds.
  * Downstream unavailability cleanly returns `503 Service Unavailable` with error code `DEPENDENCY_UNAVAILABLE`.
* **Database & Schema:**
  * Dedicated MySQL database (`resident_management_db`) managed exclusively via Flyway migration (`V1__init_schema.sql`).
  * Unique constraint on `(user_id, profile_type)` ensuring no duplicate profile types per user.
  * Indexes on `user_id`, `email` (`idx_profiles_email`), `profile_type`, and `status`.
  * Dedicated `audit_events` table for structured auditing of profile events.

---

## 🛠️ Tech Stack & Standards

* **Base Package:** `kln.ams.residentmanagement`
* **Maven GroupId:** `kln.ams`
* **Java Version:** 21 (Eclipse Temurin 21)
* **Framework:** Spring Boot 3.2.5 (Spring MVC, Spring Data JPA, Spring Security OAuth2 Resource Server, Spring Boot Actuator)
* **Security:** Nimbus JOSE JWT (RS256 signature verification and generation), `X-Request-ID` correlation tracing filter
* **Database:** MySQL 8.0, Hibernate, Flyway Migrations
* **Testing:** JUnit 5, Mockito, Spring Boot Test, H2 In-Memory Database (MySQL Mode)
* **API Documentation:** OpenAPI 3.0.3 (`docs/openapi.yaml`), Postman Collection (`Resident-Management-Service.postman_collection.json`)

---

## 🚀 Canonical API Endpoints (14 Operations)

All provider endpoints conform strictly to the canonical contract defined in `RESIDENT-MANAGEMENT-SERVICE.md`:

### 1. Resident Profile Endpoints
| API ID | Method | Path | Allowed Roles / Caller | Description |
|---|---|---|---|---|
| `RES-001` | `GET` | `/api/v1/residents` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | List resident profiles (paged, filterable by status, search, userId) |
| `RES-002` | `POST` | `/api/v1/residents` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | Create resident profile (validates user via Identity Access) |
| `RES-003` | `GET` | `/api/v1/residents/{residentId}` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER`, or Self | Get resident profile by resident ID |
| `RES-004` | `PATCH` | `/api/v1/residents/{residentId}` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER`, or Self | Update resident profile fields |

### 2. Owner Profile Endpoints
| API ID | Method | Path | Allowed Roles / Caller | Description |
|---|---|---|---|---|
| `OWN-001` | `GET` | `/api/v1/owners` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | List owner profiles (paged, filterable) |
| `OWN-002` | `POST` | `/api/v1/owners` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | Create owner profile (validates user via Identity Access) |
| `OWN-003` | `GET` | `/api/v1/owners/{ownerId}` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER`, or Self | Get owner profile by owner ID |

### 3. Tenant Profile Endpoints
| API ID | Method | Path | Allowed Roles / Caller | Description |
|---|---|---|---|---|
| `TEN-001` | `GET` | `/api/v1/tenants` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | List tenant/resident profiles (paged, filterable) |
| `TEN-002` | `POST` | `/api/v1/tenants` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | Create tenant profile (validates user via Identity Access) |
| `TEN-003` | `GET` | `/api/v1/tenants/{tenantId}` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER`, or Self | Get tenant profile by tenant ID |

### 4. Staff Profile Endpoints
| API ID | Method | Path | Allowed Roles / Caller | Description |
|---|---|---|---|---|
| `STF-001` | `GET` | `/api/v1/staff` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | List staff profiles (paged, filterable) |
| `STF-002` | `POST` | `/api/v1/staff` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | Create staff profile (validates user via Identity Access) |

### 5. Internal Service-to-Service Endpoints
| API ID | Method | Path | Authentication | Allowed Caller Services |
|---|---|---|---|---|
| `RES-INT-001` | `GET` | `/api/v1/internal/residents/{residentId}/validate` | Gateway Service JWT (`type=service`) | `property-unit-service`, `lease-occupancy-service`, `billing-payment-service`, `utility-charge-service`, `operations-service`, `community-service` |
| `RES-INT-002` | `GET` | `/api/v1/internal/users/{userId}/relationships` | Gateway Service JWT (`type=service`) | Registered Project A backend services |

---

## 📦 Global Response & Error Envelopes

### 1. Standard Success Response
HTTP 200/201 returns:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-30T12:00:00Z",
  "requestId": "7f83a9b2-3c4d-4e5f-a6b7-8c9d0e1f2a3b"
}
```

### 2. Standard Paged List Response
```json
{
  "success": true,
  "message": "Residents retrieved successfully",
  "data": {
    "items": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-09-30T12:00:00Z",
  "requestId": "7f83a9b2-3c4d-4e5f-a6b7-8c9d0e1f2a3b"
}
```

### 3. Standard Error Response
HTTP 4xx/5xx returns:
```json
{
  "success": false,
  "message": "Resident profile not found for id: uuid",
  "error": {
    "code": "RESIDENT_NOT_FOUND",
    "details": null
  },
  "timestamp": "2026-09-30T12:00:00Z",
  "requestId": "7f83a9b2-3c4d-4e5f-a6b7-8c9d0e1f2a3b"
}
```

Validation errors (`VALIDATION_ERROR`) include field-level details:
```json
{
  "success": false,
  "message": "Request validation failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "details": {
      "firstName": "firstName is required",
      "email": "email must be a valid email address"
    }
  },
  "timestamp": "2026-09-30T12:00:00Z",
  "requestId": "7f83a9b2-3c4d-4e5f-a6b7-8c9d0e1f2a3b"
}
```

### 4. Canonical Error Codes
* `VALIDATION_ERROR`: Malformed JSON or DTO validation failure (HTTP 400)
* `INVALID_TOKEN`: Missing, invalid, expired, or wrong type User JWT (HTTP 401)
* `INVALID_SERVICE_TOKEN`: Missing, invalid, or wrong type Service JWT (HTTP 401)
* `PERMISSION_DENIED`: Insufficient user role or unauthorized cross-user profile access (HTTP 403)
* `CALLER_SERVICE_NOT_ALLOWED`: Service caller not in endpoint allowlist (HTTP 403)
* `USER_NOT_FOUND`: Referenced user does not exist in Identity Access (HTTP 404)
* `RESIDENT_NOT_FOUND`: Resident profile not found (HTTP 404)
* `OWNER_NOT_FOUND`: Owner profile not found (HTTP 404)
* `TENANT_NOT_FOUND`: Tenant profile not found (HTTP 404)
* `STAFF_NOT_FOUND`: Staff profile not found (HTTP 404)
* `RESIDENT_ALREADY_EXISTS`: Resident profile already exists for user (HTTP 409)
* `OWNER_ALREADY_EXISTS`: Owner profile already exists for user (HTTP 409)
* `TENANT_ALREADY_EXISTS`: Tenant profile already exists for user (HTTP 409)
* `STAFF_ALREADY_EXISTS`: Staff profile already exists for user (HTTP 409)
* `DEPENDENCY_UNAVAILABLE`: Downstream service communication failure or timeout (HTTP 503)
* `INTERNAL_SERVER_ERROR`: Unhandled internal server exception (HTTP 500)

---

## 🗄️ Database Migrations

Managed strictly via **Flyway**:
* `src/main/resources/db/migration/V1__init_schema.sql`:
  * `profiles` table: `id`, `user_id`, `first_name`, `last_name`, `email`, `phone`, `profile_type`, `status`, `created_at`, `updated_at`.
  * Unique constraint on `(user_id, profile_type)` (`uq_user_profile_type`).
  * Indexes on `user_id`, `email` (`idx_profiles_email`), `profile_type`, and `status`.
  * `audit_events` table for structured auditing.

---

## 🚀 Getting Started

### 1. Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```

| Variable | Description | Default |
|---|---|---|
| `PORT` | HTTP server port | `8081` |
| `DB_URL` | MySQL JDBC connection URL | `jdbc:mysql://localhost:3306/resident_management_db` |
| `DB_USERNAME` | MySQL database username | `root` |
| `DB_PASSWORD` | MySQL database password | `your_mysql_password` |
| `GATEWAY_BASE_URL` | API Gateway base URL | `http://localhost:8000` |
| `GATEWAY_JWT_PUBLIC_KEY` | RS256 RSA public key for inbound JWT verification | *(Configured per env)* |
| `SERVICE_NAME` | Calling service identity | `resident-management-service` |
| `SERVICE_JWT_PRIVATE_KEY` | RS256 RSA private key for outbound service calls | *(Configured per env)* |
| `SERVICE_JWT_EXPIRES_IN` | Outbound Service JWT lifetime | `5m` |
| `CLIENT_CONNECT_TIMEOUT_MS`| RestTemplate connect timeout | `2000` |
| `CLIENT_READ_TIMEOUT_MS`   | RestTemplate read timeout | `5000` |

### 2. Building and Running Locally
Prerequisites: Java 21 (Temurin 21 recommended), Maven 3.9+
```bash
# Clean build and run automated tests
./mvnw clean test

# Run the application
./mvnw clean spring-boot:run
```
On Windows:
```cmd
.\mvnw.cmd clean test
.\mvnw.cmd clean spring-boot:run
```

### 3. Running with Docker
```bash
# Build Docker image (Java 21 multi-stage, non-root user)
docker build -t resident-management-service:latest .

# Run with docker-compose
docker-compose up -d
```

### 4. Health Probes
Spring Boot Actuator health endpoints:
* Health check: `GET /actuator/health` (`{"status":"UP"}`)
* Swagger UI: `http://localhost:8081/swagger-ui.html`
* OpenAPI JSON: `http://localhost:8081/v3/api-docs`
