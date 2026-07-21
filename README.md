# 🛒 ABTechZone — Spring Boot E-commerce API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Redisson-DC382D.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF.svg)](https://github.com/features/actions)
[![AWS](https://img.shields.io/badge/Deployed-AWS_EC2-FF9900.svg)](https://aws.amazon.com/ec2/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

A production-ready RESTful e-commerce backend built with **Java 21 + Spring Boot 4**, following a **Modular Monolith** architecture. Deployed to **AWS EC2** via a fully automated **GitHub Actions CI/CD** pipeline.

> 🚧 **Project Status:** Active Development

---

## ✨ Key Engineering Highlights

Beyond a standard CRUD project, ABTechZone tackles real-world backend challenges:

### 🔒 Distributed Locking & Race Condition Prevention

Concurrent order placement is handled with **Redisson distributed locks** over Redis:

- **User-level lock** — prevents duplicate order submission (double-click / double-submit)
- **SKU-level lock** — prevents overselling under concurrent requests
- **Voucher-level lock** — prevents voucher quota from being exceeded
- Lock keys are **sorted alphabetically** before acquisition to **eliminate deadlocks**

### 📦 Inventory Reservation Pattern

- Stock is **decremented atomically** via conditional SQL UPDATE (checks for sufficient stock at DB level)
- An `InventoryReservation` record is created with a **15-minute TTL**
- A **`@Scheduled` job** auto-reclaims expired reservations every 60 seconds, restoring stock and logging a `StockMovement` audit entry
- Full `StockMovement` audit trail for every stock change

### 🛡️ Security

- **JWT Authentication** with custom `JwtDecoder` and token blacklisting (Redis) for sign-out
- **Refresh Token** flow
- **OAuth2 Resource Server** integration
- Role-based authorization (`ADMIN` / `CUSTOMER`) with `@PreAuthorize` / `@EnableMethodSecurity`

### 🧱 Modular Monolith Architecture

8 self-contained modules with clear boundaries — ready to be extracted into microservices:

| Module      | Responsibility                                         |
| ----------- | ------------------------------------------------------ |
| `auth`      | JWT authentication, token management                   |
| `user`      | User profiles, address management                      |
| `product`   | Product catalog, SKU/variant management                |
| `category`  | Category & brand hierarchy, attribute system           |
| `cart`      | Shopping cart lifecycle                                |
| `order`     | Order processing, concurrent transaction handling      |
| `inventory` | Stock control, reservation, audit trail                |
| `voucher`   | Discount engine (fixed & percentage), usage validation |

---

## 🛠 Tech Stack

| Layer                 | Technology                                         |
| --------------------- | -------------------------------------------------- |
| **Language**          | Java 21                                            |
| **Framework**         | Spring Boot 4.0                                    |
| **Security**          | Spring Security, JWT, OAuth2 Resource Server       |
| **Persistence**       | Spring Data JPA (Hibernate 7), PostgreSQL 15       |
| **Caching / Locking** | Redis, Redisson 3.x                                |
| **Mapping**           | MapStruct 1.6 + Lombok 1.18                        |
| **Validation**        | Jakarta Bean Validation                            |
| **Testing**           | JUnit 5, Mockito, Testcontainers, Spring Boot Test |
| **Code Quality**      | JaCoCo (coverage), Spotless (Palantir format)      |
| **Containerization**  | Docker, Docker Compose                             |
| **CI/CD**             | GitHub Actions                                     |
| **Cloud**             | AWS EC2                                            |

---

## 📂 Database Architecture

### ERD

<div align="center">
  <img src="server/docs/erd.png" alt="Database Entity-Relationship Diagram (ERD)" width="90%" />
  <p><i>Entity-Relationship Diagram</i></p>
</div>

Key design decisions:

- `Product` ↔ `ProductSku` — 1:N, multi-variant support with JSON `attributes` column (Hypersistence Utils)
- `ProductSku` ↔ `Inventory` — 1:1, isolated stock tracking
- `Order` snapshots shipping address at creation time (decoupled from live `UserAddress`)
- Soft delete on `Product` and `ProductSku` via `@SQLRestriction("deleted_at IS NULL")`
- `StockMovement` append-only audit log for all inventory changes

---

## 📋 Feature Overview

### Authentication & Authorization

- [x] User Registration with BCrypt password hashing
- [x] Sign-in with JWT (access token + refresh token)
- [x] Token blacklisting on sign-out (Redis)
- [x] Role-based authorization: `ADMIN` / `CUSTOMER`

### User

- [x] User profile management
- [x] Address management (CRUD, default address)

### Product Catalog

- [x] Product CRUD with soft delete
- [x] Multi-variant SKU system (attribute-based)
- [x] Dynamic attribute & category system
- [x] Product publish / unpublish lifecycle
- [x] Slug auto-generation (Vietnamese diacritic support)
- [x] Paginated search with filtering & sorting (JPA Specifications)

### Shopping Cart

- [x] Add / update / remove cart items
- [x] Stock validation on add
- [x] Cart status lifecycle (`ACTIVE` / `ORDERED`)

### Order

- [x] Checkout review (read-only, validates stock & voucher)
- [x] Create order (distributed locking, atomic stock deduction)
- [x] Shipping address: use saved address or provide new (with optional save)
- [x] Order status history
- [x] Order history by user

### Voucher / Discount Engine

- [x] Voucher CRUD
- [x] Two discount types: `FIXED_AMOUNT` and `PERCENTAGE`
- [x] Apply scope: `ALL` or specific SKUs (`ManyToMany`)
- [x] Validations: expiry, max uses, min order value, per-user limit
- [x] Race-condition-safe usage tracking via atomic DB update

### Inventory

- [x] Atomic stock reservation on order creation
- [x] Stock movement audit trail (`StockMovement`)
- [x] Inventory reservation with 15-min TTL
- [x] Auto-reclaim expired reservations via `@Scheduled`

---

## 🧪 Testing

Tests are partitioned strictly by module and cover three layers:

| Module    | Unit Test                      | Integration Test                                                       |
| --------- | ------------------------------ | ---------------------------------------------------------------------- |
| `user`    | `AddressServiceTest` (Mockito) | `AddressRepositoryIntegrationTest`, `AddressControllerIntegrationTest` |
| `product` | —                              | `ProductIntegrationTest`                                               |
| `cart`    | `CartServiceTest` (Mockito)    | `CartIntegrationTest`                                                  |
| `order`   | `OrderServiceTest` (Mockito)   | `OrderIntegrationTest`                                                 |
| `voucher` | `VoucherValidatorUnitTest`     | `VoucherIntegrationTest`                                               |

**Testing approach:**

- **Unit tests**: Mockito `@Mock / @InjectMocks`, Given-When-Then pattern, `@DisplayName`
- **Integration tests**: Testcontainers (MySQL container), `@SpringBootTest`, `@DynamicPropertySource`
- **Coverage**: JaCoCo report — run `./mvnw clean test jacoco:report`, open `target/site/jacoco/index.html`

---

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local dev without Docker)

### Run with Docker Compose (Recommended)

```bash
# 1. Clone
git clone https://github.com/DuyAnh3223/springboot-ecommerce.git
cd springboot-ecommerce

# 2. Configure environment
cp .env.example .env   # then fill in your values

# 3. Start all services (PostgreSQL + Redis + Backend + Frontend)
docker compose up --build
```

Services will be available at:

- **Backend API**: `http://localhost:8080/abtechzone`
- **Frontend**: `http://localhost:3000`

### Run Backend Locally

```bash
cd server

# Start only infrastructure
docker compose up db redis -d

# Run Spring Boot
./mvnw spring-boot:run
```

### Environment Variables

| Variable                 | Description                                |
| ------------------------ | ------------------------------------------ |
| `POSTGRES_USER`          | PostgreSQL username                        |
| `POSTGRES_PASSWORD`      | PostgreSQL password                        |
| `POSTGRES_DB`            | Database name                              |
| `JWT_SIGNER_KEY`         | JWT signing secret                         |
| `REDIS_ADDRESS`          | Redis URL (e.g., `redis://localhost:3308`) |
| `SPRING_PROFILES_ACTIVE` | Active profile (`dev` / `prod`)            |

---

## 📂 Project Structure

```text
ABTechZone/
├── .github/workflows/
│   └── deploy.yml              # GitHub Actions CI/CD pipeline
├── client/                     # Next.js frontend
├── server/                     # Spring Boot backend
│   ├── Dockerfile              # Multi-stage production build
│   ├── Dockerfile.dev          # Hot-reload dev build
│   └── src/
│       ├── main/java/spring/abtechzone/
│       │   ├── common/
│       │   │   ├── config/     # Security, Redis, app init
│       │   │   ├── dto/        # Shared response model (ApiResponse<T>)
│       │   │   └── exception/  # GlobalExceptionHandler + ErrorCode enum
│       │   └── modules/        # 8 bounded-context modules
│       │       ├── auth/
│       │       ├── cart/
│       │       ├── category/
│       │       ├── inventory/
│       │       ├── order/
│       │       ├── product/
│       │       ├── user/
│       │       └── voucher/
│       └── test/               # Unit + Integration tests (per module)
├── docker-compose.yml          # Dev environment
└── docker-compose.prod.yml     # Production environment
```

Each module follows a consistent internal structure:

```
modules/<name>/
├── controller/     # REST endpoints
├── dto/            # Request / Response DTOs
├── entity/         # JPA entities
├── mapper/         # MapStruct mappers
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic
└── validator/      # Domain-specific validators (where applicable)
```

---

## 🔄 CI/CD Pipeline

```
Push to main → GitHub Actions
  ├── Build Spring Boot Docker image
  ├── Build Next.js Docker image
  ├── Push both images to Docker Hub
  └── SSH into AWS EC2
        ├── Pull new images
        ├── docker compose up -d
        └── Prune old images
```

---

## 📖 API Documentation

The API endpoints are fully documented using **Swagger UI / OpenAPI 3**.

### How to access:

- **Local environment:**
  Ensure the backend application is running, then navigate to:
  `http://localhost:8080/abtechzone/swagger-ui/index.html` (or `http://localhost:8080/abtechzone/swagger-ui.html`)
- **API Spec (JSON):** `http://localhost:8080/abtechzone/api-docs`

### Authentication in Swagger UI:

1. Call the `POST /auth/sign-in` endpoint in Swagger UI to authenticate (using admin - admin) and get your `token` (access token).
2. Click the **"Authorize"** button (with the lock icon) at the top right of the Swagger page.
3. Paste the JWT access token in the Value input field (Format: `<token>`) and click **Authorize**.

Postman collections are also available as backup/historical reference in [`server/docs/postman/`](server/docs/postman/).

---

## 📌 Development Roadmap

- [x] Project initialization & Modular Monolith structure
- [x] Authentication (JWT, Refresh Token, Sign-out with blacklist)
- [x] User module (profile, address management)
- [x] Product module (CRUD, SKU variants, publish lifecycle)
- [x] Category & attribute system
- [x] Cart module
- [x] Order module with distributed locking
- [x] Voucher / discount engine
- [x] Inventory reservation & audit trail
- [x] Docker & Docker Compose (dev + prod)
- [x] CI/CD pipeline (GitHub Actions → AWS EC2)
- [x] Unit & Integration tests (JaCoCo coverage)
- [x] Code formatter (Spotless)
- [x] Swagger / OpenAPI documentation
- [ ] Idempotency key for order creation
- [ ] Spring Actuator health endpoints
- [ ] Kafka event publishing (ORDER_CREATED, etc.)

---

## 📄 License

This project is created for learning and portfolio purposes. MIT License.

---

## 👨‍💻 Author

**Duy Anh**

GitHub: [DuyAnh3223](https://github.com/DuyAnh3223)
