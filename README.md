# Resident Management Service 🏢

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.3-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Architecture](https://img.shields.io/badge/Architecture-Microservice-orange)

The **Resident Management Service** (`resident-management-service`) is an authoritative core domain microservice for the University of Kelaniya Apartment Management System (AMS Project A). It manages personal profiles, contact records, and metadata for Residents, Owners, Tenants, and Staff members.

This service operates as a stateless **OAuth2 Resource Server** running on port **8081** behind the API Gateway.

---

## 🏗️ Architecture & Boundaries

- **Authoritative Domain Scope:**
  - Manages personal profile records (`first_name`, `last_name`, `email`, `phone`, `profile_type`, `status`) for Residents, Owners, Tenants, and Staff.
  - Provides internal verification endpoints for cross-service authorization and validation (`RES-INT-001`, `RES-INT-002`).
  - **Boundary Exclusions:**
    - Property and unit ownership records are strictly owned by `property-unit-service`.
    - Unit occupancy, lease agreements, and tenant assignments are strictly owned by `lease-occupancy-service`.
    - User authentication, credentials, and email change verification flows are strictly owned by `identity-access-service`.
- **Stateless Authentication:**
  - Validates API Gateway-issued RS256 JSON Web Tokens using the public key (`GATEWAY_JWT_PUBLIC_KEY`).
  - `/api/v1/**` user endpoints require signed User JWTs (`type=user`).
  - `/api/v1/internal/**` endpoints require signed Service JWTs (`type=service`) from authorized caller services.
- **Outbound Cross-Service Integration:**
  - Calls `identity-access-service` via `GET /api/v1/internal/users/{userId}/validate` (`IAM-INT-001`) with a signed Service JWT to verify user existence and active status before profile creation.
- **Database & Schema:**
  - Dedicated MySQL database (`resident_management_db`) managed exclusively via Flyway migration `V1__init_schema.sql`.

---

## 🛠️ Tech Stack & Standards

* **Base Package:** `kln.ams.residentmanagement`
* **Maven GroupId:** `kln.ams`
* **Java Version:** 21 (Eclipse Temurin 21)
* **Framework:** Spring Boot 3.2.3 (Spring MVC, Spring Data JPA, Spring Security OAuth2 Resource Server)
* **Security:** Nimbus JWT (RS256 signature verification), Request ID tracing filter
* **Database:** MySQL 8.0, Hibernate, Flyway Migrations
* **Testing:** JUnit 5, Mockito, Spring Boot Test, H2 In-Memory Database (MySQL Mode)
* **API Documentation:** OpenAPI 3.0.3 (`docs/openapi.yaml`), Postman Collection (`Resident-Management-Service.postman_collection.json`)

---

## 🚀 Canonical API Endpoints

All provider endpoints conform strictly to the target contract:

### 1. Resident Profile Endpoints
| ID | Method | Path | Required Role / Type | Description |
|---|---|---|---|---|
| `RES-001` | `POST` | `/api/v1/residents` | Authenticated User | Create resident profile (self or manager) |
| `RES-002` | `GET` | `/api/v1/residents` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | List residents (paged, filterable) |
| `RES-003` | `GET` | `/api/v1/residents/{id}` | Authenticated User | Get resident profile by ID (owner/manager) |
| `RES-004` | `PUT` | `/api/v1/residents/{id}` | Authenticated User | Update resident profile (owner/manager) |

### 2. Owner Profile Endpoints
| ID | Method | Path | Required Role / Type | Description |
|---|---|---|---|---|
| `OWN-001` | `POST` | `/api/v1/owners` | Authenticated User | Create owner profile |
| `OWN-002` | `GET` | `/api/v1/owners` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | List owner profiles (paged) |
| `OWN-003` | `GET` | `/api/v1/owners/{id}` | Authenticated User | Get owner profile by ID (owner/manager) |

### 3. Tenant Profile Endpoints
| ID | Method | Path | Required Role / Type | Description |
|---|---|---|---|---|
| `TEN-001` | `POST` | `/api/v1/tenants` | Authenticated User | Create tenant profile |
| `TEN-002` | `GET` | `/api/v1/tenants` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | List tenant profiles (paged) |
| `TEN-003` | `GET` | `/api/v1/tenants/{id}` | Authenticated User | Get tenant profile by ID (owner/manager) |

### 4. Staff Profile Endpoints
| ID | Method | Path | Required Role / Type | Description |
|---|---|---|---|---|
| `STF-001` | `POST` | `/api/v1/staff` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | Create staff profile |
| `STF-002` | `GET` | `/api/v1/staff` | `SYSTEM_ADMINISTRATOR`, `APARTMENT_MANAGER` | List staff profiles (paged) |

### 5. Internal Service-to-Service Endpoints
| ID | Method | Path | Authentication | Allowed Caller Services |
|---|---|---|---|---|
| `RES-INT-001` | `GET` | `/api/v1/internal/residents/{userId}/validate` | Service JWT (`type=service`) | `identity-access-service`, `property-unit-service`, `lease-occupancy-service`, `billing-payment-service`, `operations-service` |
| `RES-INT-002` | `GET` | `/api/v1/internal/users/{userId}/relationships` | Service JWT (`type=service`) | Authorized ecosystem services |

---

## 📦 Global Response & Error Envelopes

### 1. Success Response
HTTP 200/201 returns:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-30T10:00:00Z",
  "requestId": "req-98765-abcd"
}
```

### 2. Paged Success Response
```json
{
  "success": true,
  "message": "Residents retrieved successfully",
  "data": {
    "items": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3
  },
  "timestamp": "2026-09-30T10:00:00Z",
  "requestId": "req-98765-abcd"
}
```

### 3. Error Response
HTTP 4xx/5xx returns:
```json
{
  "success": false,
  "message": "Profile not found with id: prof_123",
  "error": {
    "code": "PROFILE_NOT_FOUND",
    "details": null
  },
  "timestamp": "2026-09-30T10:00:00Z",
  "requestId": "req-98765-abcd"
}
```
Validation errors (`VALIDATION_ERROR`) include field-level error mappings in `error.details`.

---

## 🗄️ Database Migrations

Managed strictly via **Flyway**:
- `V1__init_schema.sql`:
  - `profiles` table: `id`, `user_id`, `first_name`, `last_name`, `email`, `phone`, `profile_type`, `status`, `created_at`, `updated_at`.
  - Unique constraint on `(user_id, profile_type)`.
  - Indexes on `user_id`, `email`, `profile_type`, `status`.
  - `audit_events` table for structured auditing.

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
| `DB_PASSWORD` | MySQL database password | `your_password` |
| `IDENTITY_SERVICE_URL` | Identity Access Service base URL | `http://localhost:8080` |
| `GATEWAY_JWT_PUBLIC_KEY` | RS256 RSA public key for inbound JWT verification | *(Required in prod)* |
| `SERVICE_JWT_PRIVATE_KEY` | RS256 RSA private key for outbound service calls | *(Optional in dev)* |

### 2. Building and Running Locally
Prerequisites: Java 21, Maven 3.9+
```bash
# Run unit & integration tests
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
docker build -t resident-management-service:latest .
docker run -p 8081:8081 --env-file .env resident-management-service:latest
```

### 4. Health Probes
Spring Boot Actuator health endpoints are publicly accessible:
- Health check: `GET /actuator/health` (`{"status":"UP"}`)
- Liveness probe: `GET /actuator/health/liveness`
- Readiness probe: `GET /actuator/health/readiness`
