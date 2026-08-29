# Resource Booking System - RESTful API

A secure, production-ready RESTful API for managing resource reservations with JWT authentication and Role-Based Access Control (RBAC), built using Spring Boot, Java 17+, Spring Security, and Spring Data JPA.

---

## Overview

The system allows users to view available resources (e.g., meeting rooms, company vehicles, specialized equipment) and manage their own reservations. Administrators have full access to manage resources and oversee all reservations across the platform.

### Key Features
- **JWT-Based Authentication**: Stateless authentication with bearer tokens via `POST /auth/login`.
- **Bearer-Only API Security**: Authentication is accepted through the `Authorization: Bearer <token>` header; cookies are not used for authentication.
- **Role-Based Access Control (RBAC)**: Distinct permissions for `ROLE_ADMIN` and `ROLE_USER`.
- **User Identity Isolation**: User identity is extracted directly from the verified JWT security context, preventing client-side spoofing.
- **Resource Management**: Admins have full CRUD access; regular users have read-only access.
- **Reservation Lifecycle**: Supports `PENDING`, `CONFIRMED`, and `CANCELLED` statuses. Users can cancel their own reservations.
- **Conflict Prevention**: Validates overlapping reservations for the same resource to prevent double-booking.
- **Decimal Pricing**: Prices are stored and calculated using `BigDecimal` with 2 decimal places.
- **Multi-Criteria Filtering & Pagination**: Filter reservations by `status`, `minPrice`, and `maxPrice`, with built-in pagination (`page`, `size`) and sorting (`sort`).
- **Database Support**: In-memory H2 for local development, plus ready-to-use profiles for MySQL and PostgreSQL. The H2 console is available only with the `dev` profile.
- **API Documentation**: Interactive Swagger/OpenAPI documentation and an exported Postman collection.
- **Automated Test Suite**: Unit and integration tests covering authentication, RBAC, CRUD operations, and validation.

---

## Technology Stack

- **Language**: Java 17 / Java 21
- **Framework**: Spring Boot 3.2.3
- **Security**: Spring Security 6, JJWT 0.12.5 (HMAC-SHA256)
- **Persistence**: Spring Data JPA, Hibernate ORM
- **Database**: H2 (In-Memory default), MySQL 8.0, PostgreSQL 15
- **API Docs**: SpringDoc OpenAPI 3 / Swagger UI 2.3.0
- **Build Tool**: Maven 3.8+ / Maven Wrapper
- **Testing**: JUnit 5, MockMvc, Spring Security Test

---

## Project Structure

```
backend-developer-as-final-71072-athota/
├── pom.xml
├── README.md
├── LICENSE
├── docs/
│   └── Resource_Booking_API.postman_collection.json
├── src/
│   ├── main/
│   │   ├── java/com/exelynt/booking/
│   │   │   ├── BookingApplication.java
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java        # Seeds test users & resources
│   │   │   │   ├── OpenApiConfig.java          # Swagger OpenAPI setup
│   │   │   │   └── SecurityConfig.java         # Spring Security & RBAC rules
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java         # Login, register, me
│   │   │   │   ├── ReservationController.java  # Booking CRUD & filters
│   │   │   │   └── ResourceController.java     # Resource management
│   │   │   ├── dto/                            # Request & Response payloads
│   │   │   ├── entity/                         # User, Resource, Reservation
│   │   │   ├── exception/                      # Global exception handlers
│   │   │   ├── repository/                     # Spring Data JPA repositories
│   │   │   ├── security/                       # JWT filter, token provider, principal
│   │   │   └── service/                        # Business logic & validations
│   │   └── resources/
│   │       ├── application.yml                 # Default configuration (H2)
│   │       ├── application-mysql.yml           # MySQL profile
│   │       └── application-postgres.yml        # PostgreSQL profile
│   └── test/
│       └── java/com/exelynt/booking/
│           ├── BookingApplicationTests.java
│           └── controller/
│               ├── AuthControllerTest.java
│               ├── ReservationControllerTest.java
│               └── ResourceControllerTest.java
```

---

## Seed Data for Testing

When the application starts, test accounts and initial data are automatically created:

| Role | Username | Email | Password | Permissions |
| :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `admin@booking.com` | `Admin@123` | Full CRUD on resources & all reservations |
| **USER** | `user` | `user@booking.com` | `User@123` | View resources, manage own reservations |
| **USER** | `johndoe` | `john@booking.com` | `Password@123` | View resources, manage own reservations |

---

## Getting Started

### 1. Prerequisites
- Java Development Kit (JDK) 17 or higher
- Maven 3.8+ (or use `./mvnw` / `mvnw.cmd`)

### 2. Build & Test

```bash
# Clone the repository and checkout the assignment branch
git clone -b backend-developer-assignment-deadline-30th-sep-2026-64051-2702 https://github.com/exelynt-learning-platform/backend-developer-as-final-71072-athota.git
cd backend-developer-as-final-71072-athota

# Run automated tests
mvn clean test
```

### 3. Run Application

```bash
# Set a Base64-encoded secret with at least 32 bytes, then run locally with H2.
# PowerShell: $env:JWT_SECRET = '<base64-secret>'
export JWT_SECRET='<base64-secret>'
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Once started, the application will be accessible at `http://localhost:8080`.

- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **H2 Database Console** (development profile only): [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:mem:bookingdb`
  - Username: `sa`
  - Password: *(leave blank)*

Seed users and sample reservations are enabled only with the `dev` profile. They remain disabled for MySQL and PostgreSQL deployments unless `APP_SEED_ENABLED=true` is explicitly set.

---

## Database Configuration

### MySQL
To connect to MySQL, set environment variables and run with the `mysql` profile:

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=booking_db
export DB_USER=root
export DB_PASSWORD=root

mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### PostgreSQL
To connect to PostgreSQL, set environment variables and run with the `postgres` profile:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=booking_db
export DB_USER=postgres
export DB_PASSWORD=postgres

mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## API Endpoints Reference

### 1. Authentication (`/auth`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Log in with username/email and password to get a JWT Bearer token | No |
| `GET` | `/auth/me` | Retrieve profile of the currently authenticated user | Yes |

### 2. Resources (`/resources`)

| Method | Endpoint | Description | Allowed Roles |
| :--- | :--- | :--- | :--- |
| `GET` | `/resources` | List all resources with pagination, sorting, and type filter | `ROLE_USER`, `ROLE_ADMIN` |
| `GET` | `/resources/{id}` | Get resource details by ID | `ROLE_USER`, `ROLE_ADMIN` |
| `POST` | `/resources` | Create a new resource | `ROLE_ADMIN` only |
| `PUT` | `/resources/{id}` | Update an existing resource | `ROLE_ADMIN` only |
| `DELETE` | `/resources/{id}` | Delete a resource | `ROLE_ADMIN` only |

### 3. Reservations (`/reservations`)

| Method | Endpoint | Description | Allowed Roles |
| :--- | :--- | :--- | :--- |
| `POST` | `/reservations` | Create reservation (user identity derived from JWT token) | `ROLE_USER`, `ROLE_ADMIN` |
| `GET` | `/reservations` | List reservations with status, minPrice, maxPrice filtering and pagination (Admins see all; Users see only own) | `ROLE_USER`, `ROLE_ADMIN` |
| `GET` | `/reservations/{id}` | Get reservation details by ID | Owner or `ROLE_ADMIN` |
| `PUT` | `/reservations/{id}/status` | Update status (`CANCELLED` for user; any status for admin) | Owner or `ROLE_ADMIN` |
| `DELETE` | `/reservations/{id}` | Delete a reservation | Owner or `ROLE_ADMIN` |

---

## Example Usage (cURL)

### 1. Login to obtain JWT token
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "User@123"}'
```

### 2. List available resources
```bash
curl -X GET "http://localhost:8080/resources?page=0&size=10&sort=name,asc" \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

### 3. Create a reservation
```bash
curl -X POST http://localhost:8080/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-09-10T09:00:00",
    "endTime": "2026-09-10T12:00:00",
    "notes": "Sprint planning session"
  }'
```

### 4. Filter reservations by status and price
```bash
curl -X GET "http://localhost:8080/reservations?status=CONFIRMED&minPrice=100.00&maxPrice=500.00&page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

### 5. Cancel a reservation
```bash
curl -X PUT http://localhost:8080/reservations/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -d '{"status": "CANCELLED"}'
```

---

## Postman Collection

A ready-to-import Postman collection is available at:
`docs/Resource_Booking_API.postman_collection.json`

It includes pre-configured requests for all endpoints with environment variable placeholders for tokens and base URL.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
