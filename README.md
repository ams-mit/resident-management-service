# Resident Management Service 🏢

![Java 17](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Architecture](https://img.shields.io/badge/Architecture-Microservice-orange)

The **Resident Management Service** is a core microservice for the University of Kelaniya Apartment Management System (AMS). It acts as the authoritative boundary for Resident Profiles and Apartment Relationships.

This service operates as a stateless **OAuth2 Resource Server** running on port **8081** sitting behind the project's API Gateway.

---

## 🏗️ Architecture & Boundaries

- **Stateless Authentication:** The service independently verifies Gateway-issued RS256 JSON Web Tokens using the API Gateway's public key (`GATEWAY_JWT_PUBLIC_KEY`). It does not store passwords or manage user sessions.
  - `/api/v1/**` endpoints require `type=user` JWTs.
  - `/internal/v1/**` endpoints require `type=service` JWTs from authorized services.
- **Microservice Independence:** The service owns its own MySQL database (`resident_management_db`) and schema (managed exclusively via Flyway). Cross-service database access is strictly prohibited.
- **External Integrations:**
  - **Identity Access Service:** Outbound call `PUT /internal/v1/users/{userId}/email` with a signed Service JWT to finalize verified email changes.

---

## 🛠️ Tech Stack

* **Core:** Java 17, Spring Boot 3.2.x, Spring MVC
* **Security:** Spring Security (OAuth2 Resource Server, Nimbus JWT)
* **Database:** MySQL 8.0, Spring Data JPA, Hibernate
* **Migrations:** Flyway
* **Testing:** JUnit 5, Mockito, Testcontainers (MySQL module)
* **Documentation:** Springdoc OpenAPI (Swagger UI), Static OpenAPI 3.0 specification (`docs/openapi.yaml`)

---

## 🚀 Endpoints

All endpoints produce unified envelopes: `{ "data": ..., "meta": ... }` or `{ "error": { "code", "message" } }`.

| Method | Path | Authentication | Allowed Roles / Callers | Response Description |
|---|---|---|---|---|
| `GET` | `/api/v1/profiles/me` | User JWT (`type=user`) | Authenticated (Any role) | Current user profile |
| `PUT` | `/api/v1/profiles/me` | User JWT (`type=user`) | Authenticated (Any role) | Updated user profile |
| `POST` | `/api/v1/profiles/me/email-change` | User JWT (`type=user`) | Authenticated (Any role) | Accepted (`202 Accepted`) |
| `PUT` | `/api/v1/profiles/me/email-change/confirm` | User JWT (`type=user`) | Authenticated (Any role) | Confirmation (`200 OK`) |
| `GET` | `/api/v1/profiles/{userId}` | User JWT (`type=user`) | `SYSTEM_ADMINISTRATOR` | User profile by user ID |
| `POST` | `/api/v1/relationships` | User JWT (`type=user`) | Authenticated (Any role) | Created relationship request (`201 Created`) |
| `GET` | `/api/v1/relationships/me` | User JWT (`type=user`) | Authenticated (Any role) | List of own relationships |
| `GET` | `/api/v1/relationships` | User JWT (`type=user`) | `SYSTEM_ADMINISTRATOR` | Paged list of relationships (optional `status`) |
| `GET` | `/api/v1/relationships/{id}` | User JWT (`type=user`) | `SYSTEM_ADMINISTRATOR` | Relationship detail + unitValidation placeholder |
| `PATCH` | `/api/v1/relationships/{id}/approve` | User JWT (`type=user`) | `SYSTEM_ADMINISTRATOR` | Approved relationship |
| `PATCH` | `/api/v1/relationships/{id}/reject` | User JWT (`type=user`) | `SYSTEM_ADMINISTRATOR` | Rejected relationship with reason |
| `GET` | `/internal/v1/relationships/validate` | Service JWT (`type=service`) | Service allow-list | Relationship verification status |

---

## 📦 Response & Error Envelopes

### 1. Success Response (Single / Unpaged)
```json
{
  "data": {
    "profileId": "prof_12345",
    "userId": "usr_67890",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1-555-123456",
    "emergencyContact": "+1-555-654321"
  },
  "meta": {}
}
```

### 2. Paged List Response
Query parameters: `page` (default `0`), `size` (default `20`, max `100`). Invalid values return HTTP 400 with code `VALIDATION_ERROR`.
```json
{
  "data": [
    {
      "relationshipId": "rel_abcdef",
      "requesterUserId": "usr_123",
      "relationshipType": "TENANT_RESIDENT",
      "unitReference": "UNIT-101",
      "status": "APPROVED",
      "createdAt": "2026-09-30T10:00:00"
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 42
  }
}
```

### 3. Error Response
All errors (including Spring Security 401 and 403) return a standardized error envelope:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Reason is required"
  }
}
```
Standard codes include `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `PROFILE_NOT_FOUND`, `RELATIONSHIP_NOT_FOUND`, `RELATIONSHIP_ALREADY_DECIDED`, `EMAIL_ALREADY_IN_USE`, `INVALID_VERIFICATION_TOKEN`, and `DEPENDENCY_UNAVAILABLE`.

---

## ✉️ Email Change Flow (US-G1-18)

1. **Initiate Request:**
   - User submits `POST /api/v1/profiles/me/email-change` with `{ "newEmail": "new@example.com" }`.
   - The service creates a cryptographically secure random token and stores only its SHA-256 hash in `email_change_requests` with a 24-hour expiration.
   - Any prior pending requests for the user are invalidated.
   - Real email dispatch is handled outside this service (Case Scope §13). The raw token is logged **ONCE** at INFO level with prefix `[DEV-ONLY]` for local testing and demonstration purposes (e.g., `[DEV-ONLY] Email change verification token for user ...`). The raw token is **never** returned in any response.
   - Returns `202 Accepted` with `{ "data": { "message": "Verification required" }, "meta": {} }`.
   - Records audit event `EMAIL_CHANGE_REQUESTED`.
2. **Confirm Request:**
   - User submits `PUT /api/v1/profiles/me/email-change/confirm` with `{ "verificationToken": "..." }`.
   - The token hash is checked against the user's active, unexpired request for their authenticated user ID.
   - The service makes an outbound call to `identity-access-service` via `PUT /internal/v1/users/{userId}/email` with `{ "newEmail": "..." }` and a signed Service JWT.
   - On `200 OK` from identity, marks the request as used, logs audit action `EMAIL_CHANGED`, and returns `200 OK` with `{ "data": { "message": "Email updated" }, "meta": {} }`.
   - On `409 Conflict`, returns `409` (`EMAIL_ALREADY_IN_USE`) while leaving the token request active.
   - On identity unavailability, timeout, or 5xx, returns `503` (`DEPENDENCY_UNAVAILABLE`) while leaving the token request active for retry.

---

## 🔒 Internal Verification & Caller Allow-List (US-G1-26)

`GET /internal/v1/relationships/validate?userId=&unitReference=&relationshipType=` allows trusted services to verify tenant/owner status without retrieving personal profile data.

- **Authentication:** Must present a valid Gateway Service JWT (`type=service`).
- **Authorization Allow-List:** Caller `sub` must be in the configured allow-list:
  ```yaml
  internal-api:
    allowed-callers:
      relationship-validation:
        - property-unit-service
        - lease-occupancy-service
        - billing-payment-service
        - utility-charge-service
        - operations-service
        - community-service
  ```
- Calls with unauthorized service `sub` return `403 FORBIDDEN`.
- Response structure:
  ```json
  {
    "data": {
      "verified": true,
      "relationshipType": "TENANT_RESIDENT",
      "status": "APPROVED"
    },
    "meta": {}
  }
  ```
  `verified` is `true` **only** when an approved matching relationship exists. If no match is found, returns HTTP 200 with `verified: false` and `status: "NONE"`.

---

## 🗄️ Database Migrations

Database migrations are managed via **Flyway**:
- `V1__init_schema.sql`: Initial schema for profiles and apartment relationships.
- `V2__add_timestamps_and_audit_events.sql`: Adds `created_at` / `updated_at` columns and `audit_events` table.
- `V3__create_email_change_requests.sql`: Creates `email_change_requests` table.
- `V4__add_relationship_decision_columns.sql`: Adds `decided_by`, `decided_at`, and widens `decision_reason` on `apartment_relationships`.

---

## 📋 Known Open Items

The following architectural and cross-service items remain open:
- **Notifications ownership:** Architectural ownership of resident notification dispatch upon relationship approvals/rejections.
- **Group 2 unit contract:** Final contract alignment with Group 2 (Property Management) regarding unit reference validation, error response format, and schema semantics.
- **Gateway routes/keys:** Alignment on gateway routing configurations, path prefixes, and shared JWT verification keys in production environments.
- **Spring Boot/Java version alignment:** Harmonization of Java and Spring Boot baseline versions across all group services.

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
| `GATEWAY_JWT_PUBLIC_KEY` | Base64-encoded RSA public key for verifying user JWTs | *(Required)* |
| `SERVICE_NAME` | Service identifier for outbound requests | `resident-management-service` |
| `SERVICE_JWT_PRIVATE_KEY` | Base64 or PEM RSA private key for outbound service JWTs | *(Optional in dev)* |

### 2. Running Locally
```bash
./mvnw clean spring-boot:run
```
On Windows:
```cmd
.\mvnw.cmd clean spring-boot:run
```

### 3. Health Probes
Spring Boot Actuator health probes (permitted without authentication):
- Liveness: `GET /actuator/health/liveness` (`{"status":"UP"}`)
- Readiness: `GET /actuator/health/readiness` (`{"status":"UP"}`)

---
*Developed for the University of Kelaniya - Software Architecture and Process Models.*
