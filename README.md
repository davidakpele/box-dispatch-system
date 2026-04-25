# Box Dispatch System

A REST API service for managing dispatch boxes used to deliver small items to remote locations. Built with Java 21 and Spring Boot 4, with a Thymeleaf frontend for authentication and API interaction.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Build & Run](#build--run)
- [Docker & Observability Stack](#docker--observability-stack)
- [Seeded Data](#seeded-data)
- [Frontend](#frontend)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [Additional Features](#additional-features)
- [Monitoring & Grafana](#monitoring--grafana)
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
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Frontend | Thymeleaf |
| Database | PostgreSQL 16 |
| Cache / Idempotency | Redis 7 (Lettuce) |
| Security | Spring Security + JWT (JJWT / Nimbus) |
| ORM | Spring Data JPA / Hibernate |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Maven |
| Metrics | Micrometer + Prometheus |
| Dashboards | Grafana 10 |
| Log Aggregation | Loki + Promtail |

---

## Prerequisites

### Running locally (without Docker)

- Java 21+
- Maven 3.8+
- PostgreSQL 14+ running on `localhost:5432`
- Redis running on `localhost:6379`

### Running with Docker (recommended)

- Docker Engine 24+
- Docker Compose v2 (`docker compose`, not `docker-compose`)

No local JDK, PostgreSQL, or Redis installation needed — everything runs inside containers.

---

## Configuration

All configuration lives in `src/main/resources/application.yml`.

Key values to update before running locally:

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

Create the database before running locally:

```sql
CREATE DATABASE boxdispatch;
```

The schema is managed by Hibernate (`ddl-auto: update`) and is created automatically on first run.

When running via Docker Compose, all environment variables are injected automatically from the `.env` file — no manual edits to `application.yml` are needed.

---

## Build & Run

### Local (without Docker)

**Build:**

```bash
mvn clean package -DskipTests
```

**Run:**

```bash
java -jar target/box-dispatch-system-*.jar
```

Or with Maven directly:

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8000`.

---

## Docker & Observability Stack

The project ships with a full Docker Compose stack that runs the application alongside PostgreSQL, Redis, and the complete Grafana observability suite (Prometheus, Loki, Promtail, and exporters).

### Services

| Service | Image | Port | Purpose |
|---|---|---|---|
| **app** | built from `Dockerfile` | 8000 | Spring Boot REST API |
| **postgres** | `postgres:16-alpine` | 5432 | Primary database |
| **redis** | `redis:7-alpine` | 6379 | Idempotency & session cache |
| **prometheus** | `prom/prometheus:v2.51.2` | 9090 | Metrics scraper & time-series DB |
| **grafana** | `grafana/grafana:10.4.2` | 3000 | Dashboards & visualisation |
| **loki** | `grafana/loki:2.9.7` | 3100 | Log aggregation backend |
| **promtail** | `grafana/promtail:2.9.7` | — | Log shipper: Docker → Loki |
| **postgres-exporter** | `prometheuscommunity/postgres-exporter:v0.15.0` | 9187 | PostgreSQL metrics for Prometheus |
| **redis-exporter** | `oliver006/redis_exporter:v1.60.0` | 9121 | Redis metrics for Prometheus |

### Quick start

```bash
# 1. Copy the environment file and adjust passwords if needed
cp .env.example .env

# 2. Start everything (builds the app image on first run)
make up
# or: docker compose up -d --build
```

Wait approximately 30 seconds for the app to pass its health check, then open:

| URL | What |
|---|---|
| `http://localhost:8000` | Application (landing page) |
| `http://localhost:8000/docs` | Swagger UI |
| `http://localhost:3000` | Grafana (admin / admin) |
| `http://localhost:9090` | Prometheus |

### Useful Make commands

```bash
make up            # Start the full stack (build if needed)
make down          # Stop all services
make clean         # Stop and remove all data volumes (full reset)
make logs          # Follow logs from all services
make logs-app      # Follow application logs only
make restart-app   # Restart only the app container
make ps            # Show container health status
make reload-prometheus  # Reload Prometheus config without restart
```

### Dockerfile

The app uses a multi-stage build for a lean production image:

- **Stage 1** — Maven build with dependency caching (Eclipse Temurin 21 JDK)
- **Stage 2** — Layered JAR runtime (Eclipse Temurin 21 JRE Alpine), runs as a non-root user

JVM tuning applied at runtime:

```
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:+UseG1GC
-XX:+HeapDumpOnOutOfMemoryError
```

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

## Frontend

The application includes a Thymeleaf-based web interface accessible directly in the browser.

### Pages

| Route | Description |
|---|---|
| `http://localhost:8000/` | Landing page |
| `http://localhost:8000/register` | Create a new account |
| `http://localhost:8000/login` | Sign in with existing credentials |
| `http://localhost:8000/dashboard` | Interactive API dashboard (requires login) |

### Authentication Flow

1. Navigate to `/register` and create an account
2. Log in at `/login` — a JWT token is issued and stored in a secure HTTP-only cookie
3. You are redirected to `/dashboard` automatically on success

### Dashboard

The dashboard provides a browser-based interface to interact with all box dispatch endpoints without needing Postman or curl. From the dashboard you can:

- Create a new box
- Load items into a box
- View loaded items for a given box
- Check available boxes for loading
- Check the battery level of any box

Unauthenticated access to `/dashboard` is redirected to `/login` automatically by `ThymeleafAuthRedirectFilter`.

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

Request body:
```json
{
  "email": "user@example.com",
  "password": "password"
}
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

Per-IP rate limiting is enforced on all endpoints. When running behind Docker, the filter correctly resolves the real client IP from the `X-Forwarded-For` header to avoid all users sharing the same Docker gateway IP.

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
| `RateLimitingFilter` | Enforces per-IP token-bucket rate limits (Docker-aware) |
| `JwtAuthenticationFilter` | Validates and parses JWT Bearer tokens |

### HTTP Firewall

Spring's `StrictHttpFirewall` is configured to block backslashes and URL-encoded slashes, semicolons and percent-encoded characters, double slashes (`//`), and null bytes and line feed characters. The hostname validator also accepts Docker internal service names (e.g. `app`, `postgres`) so the firewall does not reject inter-container health checks.

### Request Logging

Every request is logged with a unique `requestId`, method, path, IP address, response status, and duration in milliseconds.

---

## Monitoring & Grafana

The Docker Compose stack ships a complete observability pipeline wired automatically on first run. No manual configuration is needed.

### Metrics pipeline

The application exposes Micrometer metrics at `/actuator/prometheus`. Prometheus scrapes this endpoint every 10 seconds and stores the time-series data. Grafana reads from Prometheus to render dashboards.

### Log pipeline

Promtail watches the Docker daemon socket and tails container logs for any container labelled `logging=promtail` (the app container is pre-labelled). Log entries are parsed against the Spring Boot console pattern, labelled by `level`, `logger`, `thread`, and `severity`, then pushed to Loki. Grafana reads from Loki for the live log panels.

### Grafana dashboards

Both dashboards are auto-provisioned and appear immediately under the **Box Dispatch System** folder in Grafana at `http://localhost:3000` (admin / admin).

**Box Dispatch System — Overview**

The primary operations dashboard covering:

| Section | What you see |
|---|---|
| At-a-glance | App UP/DOWN status, uptime, request rate, error rate %, p99 latency, JVM heap % |
| HTTP traffic | Request rate by endpoint and method; 2xx / 4xx / 5xx breakdown over time |
| Latency | p50 / p90 / p95 / p99 response time percentiles |
| Connections | Active and keep-alive Tomcat connections |
| JVM memory | Heap used vs max, non-heap, GC pause time by cause |
| HikariCP | Active / idle / pending / max pool connections; p95 acquisition time |
| PostgreSQL | Active connections, commits/s, rollbacks/s, cache hit ratio |
| Redis | Memory used vs max, commands/s, key hit/miss ratio |
| Application logs | Live ERROR and WARN log stream from Loki |

**Box Dispatch System — JVM Deep Dive**

A detailed panel for diagnosing memory pressure or GC issues, covering all JVM memory pools, GC collections per minute, thread states, and CPU usage.

### Alerting rules

Prometheus evaluates the following alert rules every 15 seconds:

| Alert | Condition | Severity |
|---|---|---|
| `AppDown` | App unreachable for > 1 minute | critical |
| `HighErrorRate` | > 5% 5xx rate over 5 minutes | warning |
| `HighResponseTime` | p95 latency > 2s for 3 minutes | warning |
| `HighJvmHeapUsage` | Heap usage > 85% for 5 minutes | warning |
| `HighThreadCount` | Live threads > 200 for 5 minutes | warning |
| `PostgresDown` | Exporter unreachable for 1 minute | critical |
| `HighDbConnections` | > 80 active connections for 5 minutes | warning |
| `RedisDown` | Exporter unreachable for 1 minute | critical |
| `RedisHighMemory` | Redis memory > 85% of max for 5 minutes | warning |

Alerts are visible in Grafana under **Alerting → Alert rules** and in the Prometheus UI at `http://localhost:9090/alerts`.

### Data retention

| Store | Default |
|---|---|
| Prometheus TSDB | 15 days |
| Loki chunks | 7 days |
| PostgreSQL | Persistent volume (no expiry) |
| Redis | Persistent volume + LRU eviction at 256 MB |

### Querying logs manually

Open **Explore** in Grafana and select the **Loki** datasource. Example LogQL queries:

```logql
# All application logs
{app="box-dispatch"}

# Errors only
{app="box-dispatch", severity="error"}

# Logs mentioning a specific box
{app="box-dispatch"} |= "BOX-001"

# Rate of error logs per minute
rate({app="box-dispatch", severity="error"}[1m])
```

### Observability file structure

```
.
├── docker-compose.yml
├── Dockerfile
├── .env.example
├── Makefile
├── init-db.sql
│
├── prometheus/
│   ├── prometheus.yml          # Scrape targets (app, postgres, redis, self)
│   └── alert.rules.yml         # 9 alerting rules
│
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/
│   │   │   └── datasources.yml # Prometheus + Loki auto-wired
│   │   └── dashboards/
│   │       └── dashboards.yml  # Folder config
│   └── dashboards/
│       ├── box-dispatch-overview.json
│       └── box-dispatch-jvm.json
│
├── loki/
│   └── loki-config.yml         # Storage, retention, schema
│
└── promtail/
    └── promtail-config.yml     # Docker socket scraping + log parsing
```

### Troubleshooting

**Grafana shows "No data"** — Check Prometheus targets at `http://localhost:9090/targets`. All jobs should show a green "UP" state. If `box-dispatch-app` is down, confirm the app started successfully with `make logs-app`.

**Logs not appearing in Loki** — Promtail needs read access to the Docker socket. On Linux, ensure your user is in the `docker` group (`sudo usermod -aG docker $USER`, then log out and back in). Verify Promtail is running with `make ps`.

**App container exits immediately** — PostgreSQL or Redis may still be starting up. The app has a `depends_on: condition: service_healthy` guard, but on slow machines the 30-second `start_period` may need increasing in `docker-compose.yml`.

**Port conflict** — If any port is already in use on your host, edit the `ports` mapping for that service in `docker-compose.yml` (left side is the host port).

---

## Testing

The project has three layers of tests covering unit, controller, and integration scenarios.

### Test structure

| File | Type | Description |
|---|---|---|
| `BoxServiceTest` | Unit (Mockito) | Service logic with all dependencies mocked |
| `BoxControllerTest` | Controller (MockMvc) | HTTP layer, request validation, auth enforcement |
| `BoxServiceIntegrationTest` | Integration (H2) | Full service + real DB with H2 in-memory |

### Test configuration

Integration tests use a separate profile backed by H2. Place the following file at `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
```

### Run all tests

```bash
mvn test
```

### Run a specific test class

```bash
mvn test -Dtest=BoxServiceTest
mvn test -Dtest=BoxControllerTest
mvn test -Dtest=BoxServiceIntegrationTest
```

### Test coverage summary

**Unit Tests (`BoxServiceTest`) — 17 tests**

| Method | Scenarios covered |
|---|---|
| `createBox` | Success, duplicate txref, weight > 500g, exactly 500g |
| `loadItems` | IDLE box, LOADING box, box not found, low battery, invalid state, duplicate codes in request, duplicate code in DB, weight exceeded, idempotent replay, lock released on failure |
| `getLoadedItems` | Success, empty list, box not found |
| `getAvailableBoxes` | Returns eligible boxes, empty list |
| `getBatteryLevel` | Success, box not found |

**Controller Tests (`BoxControllerTest`) — 18 tests**

| Endpoint | Scenarios covered |
|---|---|
| `POST /api/boxes` | 201 success, missing txref, txref too long, battery out of range, duplicate txref 409, unauthenticated 401 |
| `POST /api/boxes/{txref}/load` | 201 success, 200 idempotent, 404 not found, 422 low battery, invalid item code, invalid item name, zero weight |
| `GET /api/boxes/{txref}/items` | 200 with items, 200 empty list, 404 not found, 401 unauthenticated |
| `GET /api/boxes/available` | 200 with boxes, 200 empty, 401 unauthenticated |
| `GET /api/boxes/{txref}/battery` | 200 success, 404 not found, 401 unauthenticated |

**Integration Tests (`BoxServiceIntegrationTest`) — 15 tests**

| Area | Scenarios covered |
|---|---|
| `createBox` | Persists to DB, rejects duplicate txref |
| `loadItems` | Persists items, state transitions, remaining capacity, rejects loaded box, low battery, overweight, duplicate code across requests, unknown box |
| `getLoadedItems` | Returns correct items, empty box, unknown box |
| `getAvailableBoxes` | Filters by state and battery correctly, no eligible boxes |
| `getBatteryLevel` | Correct value from DB, unknown box |

### Manual testing

Use the dashboard at `http://localhost:8000/dashboard` after logging in, or use curl:

```bash
# Register
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# Login
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# Check available boxes (with token)
curl http://localhost:8000/api/boxes/available \
  -H "Authorization: Bearer <token>"

# Load items into a box
curl -X POST http://localhost:8000/api/boxes/BOX-001/load \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"name":"Parcel-X","weight":50.000,"code":"PARCEL_X_001"}]}'

# Check loaded items
curl http://localhost:8000/api/boxes/BOX-LOADED/items \
  -H "Authorization: Bearer <token>"

# Check battery level
curl http://localhost:8000/api/boxes/BOX-001/battery \
  -H "Authorization: Bearer <token>"
```