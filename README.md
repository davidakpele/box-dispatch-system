# Box Dispatch System

A REST API service for managing dispatch boxes used to deliver small items to remote locations. Built with Java 17 and Spring Boot.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Build & Run](#build--run)
- [Seeded Data](#seeded-data)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [Additional Features](#additional-features)
- [Testing](#testing)

---

## Overview

The service allows clients to:

- Create a box
- Load a box with items
- Check loaded items for a given box
- Check available boxes for loading
- Check the battery level of a given box

### Business Rules

- A box cannot be loaded with more weight than its limit (500g max)
- A box cannot enter `LOADING` state if its battery is below 25%
- Item names allow only letters, numbers, hyphens (`-`), and underscores (`_`)
- Item codes allow only uppercase letters, numbers, and underscores (`_`)
- Box `txref` must not exceed 20 characters

### Box State Lifecycle

```
IDLE → LOADING → LOADED → DELIVERING → DELIVERED → RETURNING → IDLE
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL |
| Cache / Idempotency | Redis (Lettuce) |
| Security | Spring Security + JWT (JJWT / Nimbus) |
| ORM | Spring Data JPA / Hibernate |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Maven |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ running on `localhost:5432`
- Redis running on `localhost:6379`

---

## Configuration

All configuration lives in `src/main/resources/application.yml`.

Key values to update before running:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/boxdispatch
    username: postgres
    password: root

  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8000

app:
  jwt:
    secret-key: <your-base64-encoded-secret>
    expiration-minutes: 1440
```

Create the database before running:

```sql
CREATE DATABASE boxdispatch;
```

The schema is managed by Hibernate (`ddl-auto: update`) and is created automatically on first run.

---

## Build & Run

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
java -jar target/box-dispatch-system-*.jar
```

Or with Maven directly:

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8000`.

---

## Seeded Data

The following boxes are pre-loaded into the database on first run (via `DataInitializer`):

| txref | Weight Limit | Battery | State |
|---|---|---|---|
| BOX-001 | 500g | 95% | IDLE |
| BOX-002 | 300g | 80% | IDLE |
| BOX-LOW-BAT | 500g | 20% | IDLE |
| BOX-DELIVERING | 500g | 60% | DELIVERING |
| BOX-LOADED | 200g | 70% | LOADED |

`BOX-LOADED` comes pre-loaded with two items:

| Name | Code | Weight |
|---|---|---|
| Parcel-A | PARCEL_A_001 | 80g |
| Parcel-B | PARCEL_B_002 | 50g |

Seeding is skipped automatically if data already exists.

---

## API Endpoints

Base URL: `http://localhost:8000/api/boxes`

All endpoints require a valid JWT Bearer token except auth endpoints.

### Create a Box

```
POST /api/boxes
```

Request body:
```json
{
  "txref": "BOX-003",
  "weightLimit": 300.000,
  "batteryCapacity": 85
}
```

Response `201 Created`:
```json
{
  "success": true,
  "message": "Box created successfully",
  "data": { ... }
}
```

---

### Load Items into a Box

```
POST /api/boxes/{txref}/load
```

Optional header for idempotency:
```
Idempotency-Key: <unique-uuid>
```

Request body:
```json
{
  "items": [
    { "name": "Parcel-C", "weight": 100.000, "code": "PARCEL_C_003" }
  ]
}
```

Response `201 Created` (first call) / `200 OK` (idempotent replay):
```json
{
  "success": true,
  "message": "Items loaded successfully",
  "data": {
    "boxTxref": "BOX-003",
    "boxState": "LOADED",
    "itemsLoaded": 1,
    "totalWeight": 100.000,
    "remainingCapacity": 200.000,
    "loadedItems": [ ... ],
    "idempotent": false
  }
}
```

---

### Check Loaded Items

```
GET /api/boxes/{txref}/items
```

Response `200 OK`:
```json
{
  "success": true,
  "data": [ ... ]
}
```

---

### Check Available Boxes for Loading

```
GET /api/boxes/available
```

Returns boxes in `IDLE` or `LOADING` state with battery ≥ 25%.

Response `200 OK`:
```json
{
  "success": true,
  "data": [ ... ]
}
```

---

### Check Battery Level

```
GET /api/boxes/{txref}/battery
```

Response `200 OK`:
```json
{
  "success": true,
  "data": {
    "txref": "BOX-001",
    "batteryCapacity": 95
  }
}
```

---

## Authentication

The API uses JWT Bearer token authentication.

### Register

```
POST /api/auth/register
```

### Login

```
POST /api/auth/login
```

Returns an `access_token` and `refresh_token`. Include the access token in all subsequent requests:

```
Authorization: Bearer <access_token>
```

Token expiry is configurable via `app.jwt.expiration-minutes` (default: 1440 minutes / 24 hours).

---

## API Documentation (Swagger UI)

Interactive API docs are available at:

```
http://localhost:8000/docs
```

Raw OpenAPI spec:

```
http://localhost:8000/v3/api-docs
```

---

## Additional Features

### Idempotency (Redis-backed)

`POST /api/boxes/{txref}/load` supports idempotent retries. Send the same `Idempotency-Key` header to safely replay a request without duplicate side effects. Responses are cached in Redis for 24 hours (configurable via `app.idempotency.ttl-hours`).

### Rate Limiting

Per-IP rate limiting is enforced on all endpoints:

| Endpoint | Limit |
|---|---|
| `POST /api/auth/login` | 5/min (burst: 10) |
| `POST /api/auth/register` | 3/hour |
| All other endpoints | 100/min |

### Security Filters

The following security filters run on every request in order:

| Filter | Purpose |
|---|---|
| `FirewallExceptionFilter` | Catches `RequestRejectedException` from Spring's `StrictHttpFirewall` |
| `BotDetectionFilter` | Blocks known scanner and bot `User-Agent` strings |
| `InputValidationFilter` | Rejects path traversal, SQL injection, and XSS patterns in URLs |
| `SecurityHeadersFilter` | Adds `X-Frame-Options`, `X-XSS-Protection`, `HSTS`, `CSP` headers |
| `RateLimitingFilter` | Enforces per-IP token-bucket rate limits |
| `JwtAuthenticationFilter` | Validates and parses JWT Bearer tokens |

### HTTP Firewall

Spring's `StrictHttpFirewall` is configured to block:
- Backslashes and URL-encoded slashes
- Semicolons and percent-encoded characters
- Double slashes (`//`)
- Null bytes and line feed characters

### Request Logging

Every request is logged with a unique `requestId`, method, path, IP address, response status, and duration in milliseconds.

---

## Testing

Run all tests:

```bash
mvn test
```

Run a specific test class:

```bash
mvn test -Dtest=BoxServiceTest
```

For manual testing, import the Swagger UI at `http://localhost:8000/docs` or use curl:

```bash
# Login
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# Check available boxes (with token)
curl http://localhost:8000/api/boxes/available \
  -H "Authorization: Bearer <token>"

# Load items
curl -X POST http://localhost:8000/api/boxes/BOX-001/load \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"name":"Parcel-X","weight":50.000,"code":"PARCEL_X_001"}]}'
```