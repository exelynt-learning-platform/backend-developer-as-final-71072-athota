<p align="center">
  <img src="./assets/hero.svg" width="100%" alt="Resource Booking API Banner">
</p>

<p align="center">
  <img alt="Java 17+" src="https://img.shields.io/badge/JAVA-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="Spring Boot 3" src="https://img.shields.io/badge/SPRING%20BOOT-3.2.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img alt="Spring Security 6" src="https://img.shields.io/badge/SPRING%20SECURITY-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">
  <img alt="Swagger OpenAPI" src="https://img.shields.io/badge/SWAGGER-OPENAPI%203-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">
  <a href="./LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/LICENSE-MIT-8b949e?style=for-the-badge"></a>
</p>

<p align="center">
  <strong>Secure RESTful API for Resource Booking with JWT Authentication and Role-Based Access Control</strong><br>
  Built with Spring Boot 3, Java 17+, Spring Security 6, JWT, Spring Data JPA, and multi-database support (MySQL, PostgreSQL, and H2).
</p>

---

## Architecture & Security Signal Flow

<p align="center">
  <img src="./assets/architecture.svg" width="100%" alt="Architecture Signal Flow">
</p>

The system strictly enforces **Role-Based Access Control (RBAC)** across all endpoints:
- **USER Identity Isolation:** User identity is derived strictly from the verified JWT security context, preventing client-side spoofing during reservation creation.
- **Conflict Prevention:** Automated schedule overlap validation prevents double-booking of resources during the same time interval.
- **Precise Decimal Pricing:** All monetary values and hourly rates are computed and stored as precise `BigDecimal` values.

## Core Capabilities

<table>
  <tr>
    <td width="50%">
      <strong>JWT Authentication &amp; RBAC</strong><br>
      Stateless authentication via <code>POST /auth/login</code>. Granular access controls for <code>ROLE_ADMIN</code> and <code>ROLE_USER</code>.
    </td>
    <td width="50%">
      <strong>Resource Management</strong><br>
      Admins have full CRUD control over resources (Rooms, Vehicles, Equipment). Regular users have read-only browsing access.
    </td>
  </tr>
  <tr>
    <td width="50%">
      <strong>Filtered Reservations</strong><br>
      Search reservations by status (<code>PENDING</code>, <code>CONFIRMED</code>, <code>CANCELLED</code>), <code>minPrice</code>, and <code>maxPrice</code>.
    </td>
    <td width="50%">
      <strong>Pagination &amp; Dynamic Sorting</strong><br>
      Spring Data Pageable integration supporting <code>page</code>, <code>size</code>, and custom <code>sort</code> parameters.
    </td>
  </tr>
</table>

## Seed Users for Testing

The system automatically initializes test accounts on startup:

| Username | Email | Password | Assigned Role | Capabilities |
| :--- | :--- | :--- | :--- | :--- |
| **`admin`** | `admin@booking.com` | `Admin@123` | `ROLE_ADMIN` | Full CRUD on Resources &amp; Reservations |
| **`user`** | `user@booking.com` | `User@123` | `ROLE_USER` | Read Resources, Create &amp; Manage Own Reservations |
| **`johndoe`** | `john@booking.com` | `Password@123` | `ROLE_USER` | Regular User Account |

---

## Quickstart & Installation

### 1. Prerequisites
- **Java 17+** (or Java 21)
- **Maven 3.8+** (or use included wrapper)

### 2. Clone & Build

```bash
# Clone the repository
git clone -b backend-developer-assignment-deadline-30th-sep-2026-64051-2702 https://github.com/exelynt-learning-platform/backend-developer-as-final-71072-athota.git
cd backend-developer-as-final-71072-athota

# Run tests and verify build
mvn clean test

# Run application locally (In-memory H2 database with seed data)
mvn spring-boot:run
```

> [!NOTE]
> The default profile runs with an in-memory **H2 Database** and pre-seeded test data, allowing immediate zero-configuration execution and testing.
> - **Swagger UI Documentation:** `http://localhost:8080/swagger-ui/index.html`
> - **OpenAPI JSON Spec:** `http://localhost:8080/v3/api-docs`
> - **H2 Database Console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:bookingdb`, Username: `sa`, Password: empty)

---

## Database Configuration

The application includes production-ready database profiles for **MySQL** and **PostgreSQL**.

### Running with MySQL

```bash
# Set environment variables (or rely on defaults)
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=booking_db
export DB_USER=root
export DB_PASSWORD=root

# Launch with MySQL profile
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### Running with PostgreSQL

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=booking_db
export DB_USER=postgres
export DB_PASSWORD=postgres

# Launch with Postgres profile
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## API Endpoints Reference

### 1. Authentication (`/auth`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Authenticate with credentials and receive JWT Bearer token | Public |
| `POST` | `/auth/register` | Register a new user account (ROLE_USER / ROLE_ADMIN) | Public |
| `GET` | `/auth/me` | Retrieve profile information of currently authenticated user | Authenticated |

### 2. Resource Management (`/resources`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/resources` | List resources with pagination, sorting &amp; type filtering | `USER`, `ADMIN` |
| `GET` | `/resources/{id}` | Get resource details by ID | `USER`, `ADMIN` |
| `POST` | `/resources` | Create a new resource | `ADMIN` only |
| `PUT` | `/resources/{id}` | Update an existing resource | `ADMIN` only |
| `DELETE` | `/resources/{id}` | Delete a resource | `ADMIN` only |

### 3. Reservation Management (`/reservations`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/reservations` | Create reservation (user identity strictly inferred from JWT) | `USER`, `ADMIN` |
| `GET` | `/reservations` | Filter reservations by `status`, `minPrice`, `maxPrice` (ADMIN sees all, USER sees only own) | `USER`, `ADMIN` |
| `GET` | `/reservations/{id}` | Get reservation by ID | Owner or `ADMIN` |
| `PUT` | `/reservations/{id}/status` | Update reservation status (`CANCELLED` for USER; any status for `ADMIN`) | Owner or `ADMIN` |
| `DELETE` | `/reservations/{id}` | Delete reservation | Owner or `ADMIN` |

---

## Sample cURL Requests

### 1. Login to obtain JWT Token

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "User@123"}'
```

### 2. Create Reservation (JWT Authentication)

```bash
curl -X POST http://localhost:8080/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-09-01T09:00:00",
    "endTime": "2026-09-01T12:00:00",
    "price": 300.00,
    "notes": "Sprint planning meeting"
  }'
```

### 3. Query Reservations with Filter &amp; Pagination

```bash
curl -X GET "http://localhost:8080/reservations?status=CONFIRMED&minPrice=100.00&maxPrice=500.00&page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

## Postman Collection

A complete Postman test suite is provided at:
[`docs/Resource_Booking_API.postman_collection.json`](./docs/Resource_Booking_API.postman_collection.json)

Import the collection into Postman, set the `baseUrl` variable to `http://localhost:8080`, and execute the pre-configured requests.

---

## License

Released under the [MIT License](./LICENSE).

<p align="center">
  <img src="./assets/live-status.svg" width="100%" alt="Live Status Footer">
</p>
