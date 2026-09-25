# Resident Management Service 🏢

![Java 17](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Architecture](https://img.shields.io/badge/Architecture-Microservice-orange)

The **Resident Management Service** is a core microservice for the University of Kelaniya Apartment Management System (AMS). It acts as the authoritative boundary for Resident Profiles and Apartment Relationships. 

This service is fully decentralized and designed to operate as a stateless **OAuth2 Resource Server** sitting behind the project's API Gateway.

---

## 🏗️ Architecture & Boundaries

- **Stateless Authentication:** The service independently verifies Gateway-issued RS256 JSON Web Tokens using the API Gateway's public key. It does not store passwords or manage user sessions.
- **Microservice Independence:** The service owns its own MySQL database (`ams_resident_db`) and schema (managed exclusively via Flyway). It strictly prohibits cross-service database access.
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
* MySQL 8.0 (If running locally without Docker)
* Docker (For running automated Testcontainers integration tests)

### 1. Environment Configuration

Copy the sample environment file to `.env` or set these in your OS context:

```bash
cp .env.example .env
```

Ensure the following variables are populated in your `.env` or IDE configuration:
* `DB_URL` (e.g., `jdbc:mysql://localhost:3306/ams_resident_db`)
* `DB_USERNAME` (e.g., `root`)
* `DB_PASSWORD` (Your local database password)
* `GATEWAY_JWT_PUBLIC_KEY` (The RSA public key from the API Gateway)

### 2. Running the Application locally

To start the service on port `8081`:

```bash
./mvnw clean spring-boot:run
```

### 3. Running Automated Tests

The service includes a robust suite of Unit and Integration tests. Integration tests leverage **Testcontainers** to dynamically spin up an isolated MySQL database container during the test phase. 

```bash
./mvnw clean verify
```
> **Note:** A running Docker daemon is required for `verify` to successfully execute Testcontainers.

---

## 📚 Documentation

### API Reference (Static)
The canonical and highly detailed API Reference document can be found in the `docs` folder:
👉 [**Resident Management API Reference (v1.0)**](./docs/RESIDENT-MANAGEMENT-API-REFERENCE-v1.0.md)

### Swagger / OpenAPI (Interactive)
When the application is running, the interactive Swagger documentation is automatically generated at:
👉 `http://localhost:8081/swagger-ui.html`

---

## 🗄️ Database Migrations

Database schema versioning is enforced via **Flyway**.
Migration scripts are located at:
`src/main/resources/db/migration/`

The database schema initializes automatically on boot. No manual table creation is required.

---
*Developed for the University of Kelaniya - Software Architecture and Process Models.*
