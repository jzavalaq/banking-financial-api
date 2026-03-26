# Banking Financial API

A production-grade banking and financial services API built with Spring Boot 3.2.x. This API provides comprehensive endpoints for customer management, account operations, financial transactions, payments, and loan management.

## Tech Stack

| Technology | Version | Description |
|------------|---------|-------------|
| Java | 21 | Programming language |
| Spring Boot | 3.2.5 | Application framework |
| Spring Security | 6.x | Authentication and authorization |
| Spring Data JPA | 3.x | Data persistence |
| JWT (jjwt) | 0.12.5 | Token-based authentication |
| PostgreSQL | 15+ | Production database |
| H2 | 2.x | Development database |
| Flyway | 10.13.0 | Database migrations |
| Lombok | Latest | Boilerplate reduction |
| springdoc-openapi | 2.5.0 | API documentation |
| Bucket4j | 8.7.0 | Rate limiting |

## Prerequisites

- Java 21 or higher
- Maven 3.9+
- PostgreSQL 15+ (for production)
- Docker (optional)

## Build Instructions

```bash
# Clone the repository
cd banking-financial-api

# Build the project
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Run tests
mvn test
```

## Run Instructions

### Development Mode (with H2)

```bash
# Run with dev profile (default)
mvn spring-boot:run

# Or with explicit profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on `http://localhost:8080`.

H2 Console available at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:banking`
- Username: `sa`
- Password: (empty)

### Production Mode (with PostgreSQL)

```bash
# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/banking
export DB_USERNAME=banking_user
export DB_PASSWORD=your_secure_password
export JWT_SECRET=your-256-bit-secret-key-here

# Run with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Docker Run Instructions

```bash
# Build the Docker image
docker build -t banking-api:latest .

# Run the container
docker run -d \
  --name banking-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/banking \
  -e DB_USERNAME=banking_user \
  -e DB_PASSWORD=your_secure_password \
  -e JWT_SECRET=your-256-bit-secret-key-here \
  banking-api:latest

# Run with Docker Compose
docker-compose up -d
```

## Quick Start with Docker Compose

```bash
# Copy the example environment file
cp .env.example .env

# Edit .env with your values (recommended to change JWT_SECRET and DB_PASSWORD)
# vim .env

# Start all services (app + database)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop services and remove volumes
docker-compose down -v
```

Services available after starting:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:5432
- H2 Console (dev profile only): http://localhost:8080/h2-console

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## API Endpoints

### Authentication

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123",
    "email": "john@example.com"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123"
  }'

# Refresh token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'

# Logout
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer your-access-token" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

### Customers

```bash
# Create customer
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "address": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA",
    "dateOfBirth": "1990-01-15",
    "taxId": "123456789"
  }'

# Get customer by ID
curl http://localhost:8080/api/v1/customers/1 \
  -H "Authorization: Bearer your-token"

# List customers with pagination
curl "http://localhost:8080/api/v1/customers?page=0&size=20&sortBy=createdAt&sortDir=desc" \
  -H "Authorization: Bearer your-token"

# Search customers
curl "http://localhost:8080/api/v1/customers/search?q=john&page=0&size=20" \
  -H "Authorization: Bearer your-token"

# Update customer
curl -X PUT http://localhost:8080/api/v1/customers/1 \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+1987654321",
    "address": "456 Oak Ave"
  }'
```

### Accounts

```bash
# Create account
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountType": "CHECKING",
    "initialDeposit": 1000.00,
    "overdraftLimit": 500.00
  }'

# Get account by ID
curl http://localhost:8080/api/v1/accounts/1 \
  -H "Authorization: Bearer your-token"

# Get account by number
curl http://localhost:8080/api/v1/accounts/number/BA1234567890 \
  -H "Authorization: Bearer your-token"

# Get account balance
curl http://localhost:8080/api/v1/accounts/BA1234567890/balance \
  -H "Authorization: Bearer your-token"

# Get accounts by customer (paginated)
curl "http://localhost:8080/api/v1/accounts/customer/1?page=0&size=20" \
  -H "Authorization: Bearer your-token"

# Update account status
curl -X PUT http://localhost:8080/api/v1/accounts/1/status \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "FROZEN",
    "reason": "Suspicious activity detected"
  }'
```

### Transactions

```bash
# Deposit
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "BA1234567890",
    "amount": 500.00,
    "description": "Cash deposit"
  }'

# Withdraw
curl -X POST http://localhost:8080/api/v1/transactions/withdraw \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "BA1234567890",
    "amount": 200.00,
    "description": "ATM withdrawal"
  }'

# Transfer
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountNumber": "BA1234567890",
    "toAccountNumber": "BA0987654321",
    "amount": 150.00,
    "description": "Rent payment"
  }'

# Get transaction by reference
curl http://localhost:8080/api/v1/transactions/TXN123456789012 \
  -H "Authorization: Bearer your-token"

# Get transaction history (paginated)
curl "http://localhost:8080/api/v1/transactions/account/BA1234567890?page=0&size=20" \
  -H "Authorization: Bearer your-token"
```

### Payments

```bash
# Create payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "BA1234567890",
    "paymentType": "BILL",
    "amount": 75.00,
    "payeeName": "Electric Company",
    "payeeAccountNumber": "EC9876543210",
    "payeeReference": "INV-2024-001",
    "description": "Monthly electricity bill"
  }'

# Get payment by ID
curl http://localhost:8080/api/v1/payments/1 \
  -H "Authorization: Bearer your-token"

# Get payment by reference
curl http://localhost:8080/api/v1/payments/reference/PAY123456789012 \
  -H "Authorization: Bearer your-token"

# Get payments by account (paginated)
curl "http://localhost:8080/api/v1/payments/account/BA1234567890?page=0&size=20" \
  -H "Authorization: Bearer your-token"

# Cancel payment
curl -X DELETE http://localhost:8080/api/v1/payments/1 \
  -H "Authorization: Bearer your-token"
```

### Loans

```bash
# Apply for a loan
curl -X POST http://localhost:8080/api/v1/loans \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "disbursementAccountNumber": "BA1234567890",
    "loanType": "PERSONAL",
    "principalAmount": 10000.00,
    "termMonths": 36,
    "purpose": "Home improvement"
  }'

# Approve or reject loan
curl -X PUT http://localhost:8080/api/v1/loans/1/approve \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "approvedInterestRate": 0.075
  }'

# Disburse loan
curl -X POST http://localhost:8080/api/v1/loans/1/disburse \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "notes": "Disbursed to checking account"
  }'

# Get loan by ID
curl http://localhost:8080/api/v1/loans/1 \
  -H "Authorization: Bearer your-token"

# Get loan by number
curl http://localhost:8080/api/v1/loans/number/LN1234567890 \
  -H "Authorization: Bearer your-token"

# Get loans by customer (paginated)
curl "http://localhost:8080/api/v1/loans/customer/1?page=0&size=20" \
  -H "Authorization: Bearer your-token"
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | JWT signing key (min 256 bits) | `change-me-in-production-minimum-256-bits-required` |
| `DB_URL` | Database URL | `jdbc:postgresql://localhost:5432/banking` |
| `DB_USERNAME` | Database username | `banking_user` |
| `DB_PASSWORD` | Database password | `banking_pass` |
| `ALLOWED_ORIGINS` | CORS allowed origins | `http://localhost:3000` |

## Health Check

```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Detailed health (requires authentication)
curl http://localhost:8080/actuator/health \
  -H "Authorization: Bearer your-token"
```

## License

This project is proprietary and confidential.
