# QueueLess AI

QueueLess AI is a production-ready full-stack starter for smart queue and appointment management. It includes JWT authentication, role-based dashboards, virtual token booking, QR token verification, email notification hooks, STOMP WebSocket queue updates, analytics, and MySQL persistence.

## Project Structure

```text
backend/   Spring Boot REST API, WebSocket, JPA, JWT, mail, QR generation
frontend/  React + React Router + Axios + Bootstrap + Chart.js application
docs/      API reference and ER diagram
```

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8+

## Backend Setup

Create a MySQL database user or use the defaults in `backend/src/main/resources/application.yml`.

```powershell
cd backend
$env:DB_URL="jdbc:mysql://localhost:3306/queueless_ai?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="Password"
$env:JWT_SECRET="replace-with-a-long-production-secret-at-least-32-chars"
mvn spring-boot:run
```

The API runs on `http://localhost:8081` by default. Swagger UI is available at `http://localhost:8081/swagger-ui.html`.
Set `SERVER_PORT=8080` if you want the conventional Spring Boot port and it is free.

Demo seeding is enabled by default:

- Admin: `admin@queueless.ai` / `admin123`
- User: `user@queueless.ai` / `user123`

Set `SEED_DEMO_DATA=false` in production.

## Frontend Setup

```powershell
cd frontend
npm install
npm run dev
```

The UI runs on `http://localhost:5009`.

Optional frontend API override:

```powershell
$env:VITE_API_URL="http://localhost:8081/api"
```

## Notifications

Email notification persistence is always enabled. SMTP sending is disabled by default so local development can run without a mail server.

Enable SMTP with:

```powershell
$env:EMAIL_NOTIFICATIONS_ENABLED="true"
$env:MAIL_HOST="smtp.example.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="..."
$env:MAIL_PASSWORD="..."
$env:MAIL_SMTP_AUTH="true"
$env:MAIL_STARTTLS_ENABLE="true"
```

## WebSocket Topics

- Endpoint: `ws://localhost:8081/ws`
- Counter queue updates: `/topic/counters/{counterId}`
- Global queue updates: `/topic/queues`
- User updates: `/topic/users/{userId}`

## Documentation

- [API Reference](docs/API.md)
- [ER Diagram](docs/ER_DIAGRAM.md)
- [MySQL Schema](backend/src/main/resources/schema.sql)
