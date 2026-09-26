# Resident Management Service 🏢

![Java 17](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Architecture](https://img.shields.io/badge/Architecture-Microservice-orange)

The **Resident Management Service** is a core microservice for the University of Kelaniya Apartment Management System (AMS). It acts as the authoritative boundary for Resident Profiles and Apartment Relationships.

This service operates as a stateless **OAuth2 Resource Server** running on port **8081** sitting behind the project's API Gateway.

---

## 🏗️ Architecture & Boundaries

- **Stateless Authentication:** The service independently verifies Gateway-issued RS256 JSON Web Tokens using the API Gateway's public key. It does not store passwords or manage user sessions.
- **Microservice Independence:** The service owns its own MySQL database (`resident_management_db`) and schema (managed exclusively via Flyway). It strictly prohibits cross-service database access.
- **External Integrations:**
  - **Property Service:** Validates the physical existence of apartment units via synchronous HTTP calls.
  - **Identity Service:** Triggers secure, asynchronous-acting email synchronizations upstream.

---

## 🛠️ Tech Stack

* **Core:** Java 17, Spring Boot 3.2.x, Spring MVC
* **Security:** Spring Security (OAuth2 Resource Server, JWT)
* **Database:** MySQL 8.0, Spring Data JPA, Hibernate
* **Migrations:** Flyway
* **Testing:** JUnit 5, Mockito, Testcontainers (MySQL module)
* **Documentation:** Springdoc OpenAPI (Swagger UI)

---

## 🚀 Getting Started

### Prerequisites
* JDK 17
* Maven (`./mvnw` wrapper included)
* MySQL 8.0 (if running locally without Docker)
* Docker (for running local Docker Compose or Testcontainers integration tests)

### 1. Environment Configuration

Copy the sample environment file to `.env`:

```bash
cp .env.example .env
```

The environment variables defined in `.env.example` are:

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
| `SERVICE_JWT_EXPIRES_IN` | Outbound JWT expiration duration | `5m` |
| `CLIENT_CONNECT_TIMEOUT_MS`| HTTP client connection timeout in milliseconds | `3000` |
| `CLIENT_READ_TIMEOUT_MS` | HTTP client read timeout in milliseconds | `5000` |

### 2. Running with Docker Compose

To spin up both the service and MySQL 8 using Docker Compose:

```bash
docker compose up -d
```

This starts:
- `resident-db`: MySQL 8 on port `3306` with database `resident_management_db` and healthchecks.
- `resident-management-service`: Spring Boot application on port `8081`.

### 3. Running Locally

Ensure a MySQL instance is running with database `resident_management_db`, then run:

```bash
./mvnw clean spring-boot:run
```

On Windows:
```cmd
.\mvnw.cmd clean spring-boot:run
```

### 4. Running Automated Tests

Run unit and integration tests:

```bash
./mvnw test
```

Run full verification (including Testcontainers integration tests):

```bash
./mvnw clean verify
```

> **Note:** A running Docker daemon is required for Testcontainers integration tests.

---

## 🩺 Health Probes

Spring Boot Actuator exposes dedicated Kubernetes-compatible health probes (permitted without JWT authentication):

- **Liveness probe:** `GET /actuator/health/liveness` (returns 200 `{"status":"UP"}`)
- **Readiness probe:** `GET /actuator/health/readiness` (validates database connectivity, returns 200 `{"status":"UP"}`)

No other actuator endpoints are publicly exposed.

---

## 📚 Documentation & API Testing

### Swagger / OpenAPI (Interactive)
When the application is running, the interactive Swagger documentation is available at:
👉 `http://localhost:8081/swagger-ui.html`

### Postman Collection
A preconfigured Postman collection is available at the root of the repository:
👉 `Resident-Management-Service.postman_collection.json`

---

## 📦 Response Format

All responses from `/api/v1/**` and `/internal/v1/**` follow the unified response envelopes aligned with `identity-access-service`.

### 1. Success Response (Single / Unpaged)
Endpoints returning a single entity or unpaged collections wrap their payload under `data`, with `meta` defaulting to an empty JSON object `{}`:
```json
{
  "data": {
    "userId": "usr_12345",
    "profileType": "RESIDENT",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1234567890",
    "statusInfo": "ACTIVE"
  },
  "meta": {}
}
```

### 2. Paged List Response
Endpoints supporting pagination (e.g. `GET /api/v1/residents?page=0&size=20`) return a list under `data` and standard metadata under `meta` with `page`, `size`, and `totalElements`:
```json
{
  "data": [
    {
      "id": "res_123",
      "userId": "usr_12345",
      "profileType": "RESIDENT",
      "firstName": "John",
      "lastName": "Doe",
      "phone": "+1234567890",
      "emergencyContact": "Jane Doe - 0987654321"
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 1
  }
}
```

### 3. Error Response
All error responses (400, 401, 403, 404, 409, 500, 503) return a standardized `{ "error": { "code", "message" } }` envelope. 500 errors never leak stack traces or SQL details.
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Page index must not be negative"
  }
}
```

---

## ✉️ Email Change Flow (US-G1-18)

The email change flow allows users to securely update their primary email address across services:
1. **Initiate Request:**
   - User submits `POST /api/v1/profiles/me/email-change` with `{ "newEmail": "new@example.com" }`.
   - The service generates a cryptographically secure token (at least 32 bytes, URL-safe) and stores only its SHA-256 hash in `email_change_requests` with a 24-hour expiration.
   - Any prior unused requests for the user are invalidated.
   - Real email transmission is out of scope (Case Scope §13). The raw token is logged **ONCE** at INFO level with prefix `[DEV-ONLY]` for testing and demonstration purposes (e.g. `[DEV-ONLY] Email change verification token for user ...`). The raw token is never returned in any API response.
   - Returns `202 Accepted` with `{ "data": { "message": "Verification required" }, "meta": {} }`.
   - Records audit event `EMAIL_CHANGE_REQUESTED` (no token in details).
2. **Confirm Request:**
   - User submits `PUT /api/v1/profiles/me/email-change/confirm` with `{ "verificationToken": "..." }`.
   - The token hash is verified against the user's active, unexpired, and unused request for their authenticated user ID.
   - Invalid, expired, used, or cross-user tokens return `400` with code `INVALID_VERIFICATION_TOKEN`.
   - The service issues an internal call to `identity-access-service` via `PUT /internal/v1/users/{userId}/email` with `{ "newEmail": "..." }` and a signed Service JWT.
   - On `200 OK` from identity, the request is marked as used, an `EMAIL_CHANGED` audit event is logged, and `200 OK` is returned with `{ "data": { "message": "Email updated" }, "meta": {} }`.
   - On `409 Conflict` (email already in use), returns `409` (`EMAIL_ALREADY_IN_USE`) while keeping the request unused.
   - On identity unavailability, timeout, or 5xx, returns `503` (`DEPENDENCY_UNAVAILABLE`) while keeping the request unused for retry.

---

## 🗄️ Database Migrations

Database schema versioning is managed via **Flyway**.
Migration scripts are located at:
`src/main/resources/db/migration/`

- `V1__init_schema.sql`: Initial schema for profiles and apartment relationships.
- `V2__add_timestamps_and_audit_events.sql`: Adds `created_at` / `updated_at` columns and the `audit_events` table.

The database schema initializes automatically on boot. No manual table creation is required.

---

## 📋 Known Open Items

The following items are pending team/cross-service decisions:
- **Response envelope format:** Alignment on a standardized API response wrapper across all microservices (e.g., `{ success, data, error }`).
- **Gateway re-signing vs direct Identity key:** Finalizing whether internal services verify JWTs re-signed by API Gateway or verify tokens directly using Identity Access Service's public key.
- **Service JWT issuance:** Outbound service-to-service authentication contract and token generation mechanism awaiting team agreement.
- **Email-change owner:** Clarification on authoritative lifecycle ownership for user email address modifications between Identity Access Service and Resident Management Service.
- **Notifications ownership:** Architectural ownership of resident notification dispatch upon relationship approvals/rejections.
- **Group 2 unit contract:** Contract alignment with Group 2 (Property Management) regarding unit reference validation, error response format, and schema semantics.

---
*Developed for the University of Kelaniya - Software Architecture and Process Models.*
