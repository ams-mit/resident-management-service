# Project A — JWT Security Standard

## Purpose

This document defines the canonical JWT authentication, authorization, signing, trust, validation and service-to-service security model for Project A.

The standard applies to:

- `identity-access-service`;
- `api-gateway`;
- all backend services;
- user authentication;
- internal service authentication;
- Gateway-issued JWTs; and
- protected API requests.

---

## Security architecture

Project A has two authenticated request flows.

### User request

```text
Frontend
    |
    | User JWT
    v
API Gateway
    |
    | verify Identity Access signature
    | create NEW Gateway JWT
    v
Backend Service
    |
    | verify Gateway signature
    | authorize user
    v
Business operation
```

### Internal service request

```text
Service A
    |
    | Service JWT
    v
API Gateway
    |
    | verify Service A signature
    | create NEW Gateway JWT
    v
Service B
    |
    | verify Gateway signature
    | authorize calling service
    v
Business operation
```

The API Gateway is the central trust boundary for backend requests.

---

## Cryptographic standard

All Project A JWTs use:

```text
Algorithm: RS256
Key type: RSA asymmetric key pair
Token type: JWT
```

The private key signs a JWT.

The corresponding public key verifies the JWT.

Every JWT must use:

```json
{
  "alg": "RS256",
  "typ": "JWT"
}
```

Services must reject tokens using an unsupported signing algorithm.

---

## JWT types

Every Project A JWT contains a `type` claim.

Supported values:

```text
user
service
```

### User JWT

A User JWT represents an authenticated application user.

```json
{
  "sub": "user-uuid",
  "type": "user",
  "roles": [
    "TENANT_RESIDENT"
  ],
  "iat": 1750000000,
  "exp": 1750001800
}
```

### Service JWT

A Service JWT represents an authenticated backend service.

```json
{
  "sub": "resident-management-service",
  "type": "service",
  "iat": 1750000000,
  "exp": 1750000300
}
```

A Service JWT does not contain user roles.

---

## JWT claims

### User JWT claims

Required:

| Claim | Meaning |
|---|---|
| `sub` | User UUID |
| `type` | `user` |
| `roles` | Canonical user roles |
| `iat` | Issued-at Unix timestamp |
| `exp` | Expiration Unix timestamp |

### Service JWT claims

Required:

| Claim | Meaning |
|---|---|
| `sub` | Calling service name |
| `type` | `service` |
| `iat` | Issued-at Unix timestamp |
| `exp` | Expiration Unix timestamp |

---

## Claims not included

Project A JWTs must not contain:

- passwords;
- password hashes;
- private keys;
- secrets;
- sensitive credentials;
- sensitive profile data;
- apartment/unit ownership data;
- tenancy data;
- occupancy data;
- business-domain records;
- permissions as a second authorization model;
- `kid`;
- `iss`;
- `aud`.

JWTs provide authenticated identity context.

JWTs do not replace domain data.

---

## Canonical user roles

User JWT `roles` values must use the canonical Project A role identifiers:

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

Legacy role values must not be issued in new JWTs.

---

## Key ownership

### Identity Access

`identity-access-service` owns:

```text
Identity Access Private Key
Identity Access Public Key
```

The Identity Access private key is used only to sign User JWTs.

The Identity Access public key is trusted by the API Gateway.

Identity Access does not need the private keys of other services.

### API Gateway

`api-gateway` owns:

```text
Gateway Private Key
Gateway Public Key
```

The Gateway private key is used to sign Gateway JWTs.

The Gateway public key is trusted by all backend services.

The Gateway also stores the public keys of registered backend services so it can verify incoming Service JWTs.

### Backend service

Every backend service owns its own:

```text
Service Private Key
Service Public Key
```

The service private key signs Service JWTs.

The service public key is registered with the API Gateway.

A backend service does not need another backend service's public or private key.

---

## Trust relationships

```text
Identity Access Public Key
        |
        v
API Gateway


Service A Public Key
        |
        v
API Gateway

Service B Public Key
        |
        v
API Gateway

...


Gateway Public Key
        |
        +------------------+
        |                  |
        v                  v
Backend Service A     Backend Service B
        |
        +----> all backend services
```

Canonical trust model:

```text
Gateway trusts Identity Access for User JWTs.
Gateway trusts registered services for Service JWTs.
Backend services trust the Gateway for Gateway JWTs.
Backend Service B does not directly trust Service A.
```

---

## User login flow

```text
Frontend
    |
    | credentials
    v
API Gateway
    |
    v
Identity Access Service
```

Identity Access:

1. validates the credentials;
2. verifies that the account is allowed to authenticate;
3. creates a User JWT;
4. signs the User JWT with the Identity Access private key; and
5. returns the User JWT through the authentication flow.

Example:

```json
{
  "sub": "user-uuid",
  "type": "user",
  "roles": [
    "TENANT_RESIDENT"
  ],
  "iat": 1750000000,
  "exp": 1750001800
}
```

---

## User request flow

The frontend sends:

```http
Authorization: Bearer <USER_JWT>
```

The Gateway validates the User JWT.

Validation sequence:

```text
Receive Authorization header
        |
        v
Parse JWT structure
        |
        v
Confirm algorithm = RS256
        |
        v
Verify Identity Access signature
        |
        v
Check type = user
        |
        v
Check required claims
        |
        v
Check expiration
        |
        v
Apply Gateway-level authorization/routing rules
        |
        v
Create NEW Gateway JWT
        |
        v
Forward Gateway JWT to backend
```

Claims must never be trusted before successful signature verification.

---

## Gateway User JWT

After validating the User JWT, the Gateway creates a new JWT.

Example:

```json
{
  "sub": "user-uuid",
  "type": "user",
  "roles": [
    "TENANT_RESIDENT"
  ],
  "iat": 1750000000,
  "exp": 1750000300
}
```

The Gateway:

- signs the new token with the Gateway private key;
- copies only trusted and required information;
- creates a new expiration time;
- does not blindly copy arbitrary claims.

The original User JWT is never forwarded to backend services.

---

## Backend user request validation

The Gateway sends:

```http
Authorization: Bearer <GATEWAY_JWT>
```

The backend service validates:

```text
1. Require Authorization header
2. Parse JWT structure
3. Confirm algorithm = RS256
4. Verify Gateway signature
5. Check type = user
6. Check required claims
7. Check expiration
8. Authorize user role
9. Validate required business/domain relationship
10. Process request
```

Required user claims at the backend:

```text
type = user
sub = valid user UUID
roles = valid canonical role array
iat = valid Unix timestamp
exp = valid Unix timestamp
```

---

## Internal service authentication

For a service-to-service request:

```text
Service A
    |
    | Service JWT
    v
API Gateway
    |
    | Gateway JWT
    v
Service B
```

Service A creates a Service JWT using its own private key.

Example:

```json
{
  "sub": "resident-management-service",
  "type": "service",
  "iat": 1750000000,
  "exp": 1750000300
}
```

Service A sends:

```http
Authorization: Bearer <SERVICE_A_JWT>
```

---

## Gateway validation of Service JWT

The Gateway validates:

```text
1. Require Authorization header
2. Parse JWT structure
3. Confirm algorithm = RS256
4. Read claimed service identifier for key selection
5. Select the registered public key
6. Verify the Service JWT signature
7. Check type = service
8. Check required claims
9. Check expiration
10. Check whether the service may call the requested endpoint
11. Create NEW Gateway JWT
```

The `sub` value is not trusted as authenticated identity until signature verification succeeds.

Required claims:

```text
type = service
sub = registered service name
iat = valid Unix timestamp
exp = valid Unix timestamp
```

---

## Gateway Service JWT

After successful Service JWT validation, the Gateway creates a new JWT.

Example:

```json
{
  "sub": "resident-management-service",
  "type": "service",
  "iat": 1750000000,
  "exp": 1750000300
}
```

The Gateway signs it with the Gateway private key.

The original Service A JWT is never forwarded to Service B.

---

## Backend internal request validation

The Gateway sends:

```http
Authorization: Bearer <GATEWAY_JWT>
```

Service B validates:

```text
1. Require Authorization header
2. Parse JWT structure
3. Confirm algorithm = RS256
4. Verify Gateway signature
5. Check type = service
6. Check required claims
7. Check expiration
8. Identify authenticated calling service from sub
9. Check service-level authorization
10. Process request
```

Required internal claims:

```text
type = service
sub = authenticated calling service
iat = valid Unix timestamp
exp = valid Unix timestamp
```

Service B does not need Service A's public key.

---

## User vs service authentication

| Request | Initial token | `type` | `sub` | Backend token signer |
|---|---|---|---|---|
| User request | User JWT | `user` | User UUID | Gateway |
| Internal request | Service JWT | `service` | Service name | Gateway |

Therefore:

```text
type = user
    -> user authentication context

type = service
    -> internal service authentication context
```

---

## Universal backend validation

Every backend service receiving a Gateway JWT follows:

```text
Receive request
      |
      v
Extract JWT
      |
      v
Validate JWT structure
      |
      v
Verify Gateway signature
      |
      v
Check type
      |
      v
Check required claims
      |
      v
Check expiration
      |
      v
Authorize user or service
      |
      v
Process request
```

No backend service may authorize a request using claims from an invalid or unverified token.

---

## Authentication vs authorization

Authentication answers:

```text
Who is this?
```

Authorization answers:

```text
Is this identity allowed to perform this operation?
```

For users:

```text
JWT authentication
    |
    +-- user identity
    +-- canonical roles
    |
    v
Endpoint role policy
    |
    v
Business/domain relationship
    |
    v
Allow / Deny
```

For services:

```text
JWT authentication
    |
    +-- authenticated service
    |
    v
Endpoint allowed-caller policy
    |
    v
Allow / Deny
```

---

## Business-domain authorization

A valid JWT does not prove:

- apartment ownership;
- tenancy;
- occupancy;
- facility eligibility;
- work-order assignment;
- invoice ownership;
- any other domain relationship.

When an endpoint requires such a relationship, the relevant owning service must validate the current relationship.

Do not place these relationships into JWT claims.

---

## Token validation failure rules

Reject the request when:

- `Authorization` is missing;
- Bearer token is missing;
- Bearer token is malformed;
- JWT structure is invalid;
- algorithm is not `RS256`;
- signature verification fails;
- token `type` is missing;
- token `type` is unexpected;
- required claims are missing;
- required claims have invalid types;
- `exp` is missing;
- `exp` is invalid;
- token is expired;
- a Service JWT identifies an unregistered service;
- a User JWT is used as a Service JWT;
- a Service JWT is used as a User JWT.

Authentication failures return:

```http
401 Unauthorized
```

An authenticated request that fails authorization returns:

```http
403 Forbidden
```

---

## Token lifetimes

Recommended lifetimes:

```text
User access JWT: 30 minutes
Service JWT:      5 minutes
Gateway JWT:      5 minutes
```

These values may be configured per environment but must remain bounded.

Gateway JWTs must remain short-lived.

---

## Role changes

Roles are included in the User JWT when it is issued.

If a user's role changes after a token has been issued, the existing token may contain the previous role until it expires.

The standard 30-minute User JWT lifetime limits this window.

For sensitive operations, the relevant service may perform an additional current-state authorization check through Identity Access.

---

## Account status

A user account must be active for successful authentication.

Authentication must not issue a normal User JWT for an inactive or disabled account.

Existing tokens may remain cryptographically valid until expiration unless a future revocation mechanism is introduced.

Sensitive operations may perform current account-state validation when required by the service contract.

---

## Logout

Initial logout behavior:

```text
Frontend deletes the User JWT.
```

The JWT remains cryptographically valid until expiration unless a future token-revocation mechanism is introduced.

Logout does not require direct modification of backend business data.

---

## HTTPS

Production and deployed environments must use HTTPS for:

```text
Frontend → Gateway
Gateway → Identity Access
Gateway → Backend Service
Service → Gateway
```

JWTs must not be transmitted over unencrypted HTTP in production.

---

## Private key security

Private keys must:

- never be committed to Git;
- never be placed directly in source code;
- never be shared between services;
- never be logged;
- never be returned in API responses;
- never be exposed through Swagger;
- never be stored in frontend code.

Use environment variables or a secure deployment secret mechanism.

---

## Environment configuration

### Identity Access

```env
SERVICE_NAME=identity-access-service
JWT_ALGORITHM=RS256
JWT_PRIVATE_KEY=<private-key>
JWT_ACCESS_TOKEN_EXPIRES_IN=30m
```

### API Gateway

```env
SERVICE_NAME=api-gateway
JWT_ALGORITHM=RS256

IDENTITY_JWT_PUBLIC_KEY=<identity-public-key>

GATEWAY_JWT_PRIVATE_KEY=<gateway-private-key>
GATEWAY_JWT_EXPIRES_IN=5m

<TRUSTED_SERVICE>_PUBLIC_KEY=<service-public-key>
```

The trusted service key configuration must include all registered Project A backend services.

### Backend service

Example:

```env
SERVICE_NAME=resident-management-service
JWT_ALGORITHM=RS256

GATEWAY_JWT_PUBLIC_KEY=<gateway-public-key>

SERVICE_JWT_PRIVATE_KEY=<service-private-key>
SERVICE_JWT_EXPIRES_IN=5m
```

Each backend service has its own private key.

---

## Environment files

Real environment files containing secrets must not be committed.

Use:

```text
.env.example
```

with placeholders.

Example:

```env
JWT_PRIVATE_KEY=<your-private-key>
GATEWAY_JWT_PUBLIC_KEY=<gateway-public-key>
```

---

## Public key distribution

```text
Identity Access Public Key
        |
        v
API Gateway


Service A Public Key
        |
        v
API Gateway


Service B Public Key
        |
        v
API Gateway


Gateway Public Key
        |
        +-------------------+
        |                   |
        v                   v
Backend Service A     Backend Service B
```

Only public keys are distributed between trust boundaries.

Private keys remain with their owning component.

---

## JWT transport rules

JWTs must be transmitted through the HTTP Authorization header:

```http
Authorization: Bearer <JWT>
```

Do not:

- place access tokens in URL query parameters;
- place JWTs in URL paths;
- log Authorization headers;
- expose JWTs in application error messages;
- send original User JWTs to backend services after Gateway re-signing;
- send original Service JWTs to downstream services after Gateway re-signing.

---

## Gateway responsibilities

The API Gateway is responsible for:

- central routing;
- User JWT verification;
- Service JWT verification;
- Identity Access trust;
- registered service trust;
- Gateway JWT creation;
- forwarding authenticated requests;
- authentication-context validation;
- service-level trust enforcement;
- consistent authentication failures;
- request authentication boundaries.

The Gateway does not own:

- user credentials;
- user profiles;
- roles as a business resource;
- apartment ownership;
- leases;
- invoices;
- payments;
- maintenance;
- facilities;
- visitors;
- announcements;
- notifications;
- other business-domain data.

---

## Identity Access responsibilities

`identity-access-service` is responsible for:

- user authentication;
- user account management;
- role management;
- User JWT creation;
- User JWT signing;
- maintaining the Identity Access private key;
- providing the Identity Access public key to the Gateway;
- account-status validation;
- identity APIs required by other services.

Identity Access does not authenticate backend services in the internal service-to-service flow.

---

## Backend service responsibilities

Every backend service must:

- protect its protected endpoints;
- verify Gateway JWT signatures;
- verify the JWT algorithm;
- check JWT type;
- check required claims;
- check expiration;
- authorize user roles;
- authorize calling services;
- validate required business relationships;
- use documented APIs for cross-service communication;
- never access another service's database directly;
- never trust unverified JWT claims.

---

## Service-to-service authorization

Authentication identifies the calling service.

Authorization determines whether that service is allowed to call the endpoint.

Example:

```text
sub = resident-management-service
type = service
        |
        v
Verify Gateway signature
        |
        v
Check endpoint allowed callers
        |
        +---- allowed ----> continue
        |
        +---- denied -----> 403 Forbidden
```

Allowed calling services must be documented in the service-specific API contract.

---

## JWT propagation rules

### User request

```text
Frontend
   |
   | User JWT
   v
Gateway
   |
   | NEW Gateway JWT
   v
Backend
```

### Internal request

```text
Service A
   |
   | Service JWT
   v
Gateway
   |
   | NEW Gateway JWT
   v
Service B
```

The original token is never forwarded beyond the Gateway trust boundary.

---

## Security error behavior

Authentication failures use:

```http
401 Unauthorized
```

Examples:

```text
MISSING_TOKEN
INVALID_TOKEN
INVALID_SIGNATURE
UNSUPPORTED_ALGORITHM
TOKEN_EXPIRED
INVALID_TOKEN_TYPE
INVALID_TOKEN_CLAIMS
UNREGISTERED_SERVICE
```

Authorization failures use:

```http
403 Forbidden
```

Examples:

```text
ROLE_NOT_ALLOWED
SERVICE_NOT_ALLOWED
BUSINESS_SCOPE_NOT_ALLOWED
```

Error responses must follow the global API error envelope.

Do not expose cryptographic or implementation details in client-facing error messages.

---

## Security logging

Security events should be logged with the request ID and relevant non-sensitive context.

Examples:

```text
LOGIN_SUCCESS
LOGIN_FAILURE
JWT_VALIDATION_FAILURE
TOKEN_EXPIRED
USER_AUTHORIZATION_DENIED
SERVICE_AUTHORIZATION_DENIED
```

Never log:

- passwords;
- JWT contents;
- Authorization headers;
- private keys;
- signing secrets;
- sensitive credentials.

---

## Key rotation

Key rotation must preserve the trust model.

When a signing key is rotated:

1. Generate a new RSA key pair.
2. Securely deploy the new private key to its owner.
3. Distribute the new public key to the required trusted component.
4. Update verification configuration.
5. Maintain the old public key only for the approved overlap period if existing tokens require it.
6. Remove the old public key after all tokens signed with the old key can no longer be valid.
7. Verify User JWT, Service JWT and Gateway JWT flows after rotation.

A key rotation must not require sharing private keys between components.

---

## Security implementation checklist

### Identity Access

- [ ] Generate RSA key pair.
- [ ] Store private key securely.
- [ ] Configure RS256.
- [ ] Issue User JWTs.
- [ ] Include `sub`.
- [ ] Include `type=user`.
- [ ] Include canonical `roles`.
- [ ] Include `iat`.
- [ ] Include `exp`.
- [ ] Use bounded User JWT lifetime.
- [ ] Provide public key to Gateway.
- [ ] Never expose private key.
- [ ] Do not issue tokens for inactive accounts.

### API Gateway

- [ ] Store Identity Access public key.
- [ ] Store trusted service public keys.
- [ ] Store Gateway private key securely.
- [ ] Verify User JWTs.
- [ ] Verify Service JWTs.
- [ ] Verify signature before trusting claims.
- [ ] Check algorithm.
- [ ] Check `type`.
- [ ] Check required claims.
- [ ] Check expiration.
- [ ] Check service registration.
- [ ] Check allowed service calls.
- [ ] Create new Gateway JWT.
- [ ] Sign Gateway JWT with Gateway private key.
- [ ] Forward only the Gateway JWT.
- [ ] Never expose Gateway private key.

### Backend services

- [ ] Store Gateway public key.
- [ ] Store service private key.
- [ ] Verify Gateway JWT.
- [ ] Verify signature before trusting claims.
- [ ] Check algorithm.
- [ ] Check `type`.
- [ ] Check required claims.
- [ ] Check expiration.
- [ ] Authorize user roles.
- [ ] Authorize calling services.
- [ ] Validate business relationships.
- [ ] Create Service JWTs for internal calls.
- [ ] Send internal calls through the Gateway.
- [ ] Never share private keys.
- [ ] Never access another service database directly.

---

## Canonical security flow

```text
========================================================
                    USER REQUEST
========================================================

Frontend
   |
   | User JWT
   | type = user
   v
API Gateway
   |
   | Verify Identity Access signature
   | Check type / claims / expiration
   | Apply Gateway rules
   |
   | Create NEW Gateway JWT
   | type = user
   v
Backend Service
   |
   | Verify Gateway signature
   | Check type / claims / expiration
   | Authorize role
   | Validate business relationship
   v
Business operation


========================================================
                 INTERNAL REQUEST
========================================================

Service A
   |
   | Service JWT
   | type = service
   v
API Gateway
   |
   | Verify Service A signature
   | Check service registration
   | Check type / claims / expiration
   | Check allowed caller
   |
   | Create NEW Gateway JWT
   | type = service
   v
Service B
   |
   | Verify Gateway signature
   | Check type / claims / expiration
   | Authorize calling service
   v
Business operation
```

## Final security rule

```text
Identity Access authenticates users.
Each backend service authenticates itself to the Gateway.
The Gateway verifies incoming tokens.
The Gateway creates trusted Gateway JWTs.
Backend services verify Gateway JWTs.
Backend services perform authorization.
Business-domain relationships remain authoritative in their owning services.
```
