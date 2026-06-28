<div align="center">
  
  # 🚀 Q-Smart (Manage Queue Through Digital)
  
  **A Production-Ready Full-Stack Queue & Appointment Management Platform**
  
  *Empowering organizations. Saving time. Redefining the waiting experience.*

  [![Spring Boot](https://img.shields.io/badge/Java_Spring_Boot-3.3-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![React](https://img.shields.io/badge/React-18.3-61DAFB?style=flat-square&logo=react&logoColor=black)](https://reactjs.org/)
  [![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://mysql.com/)
  [![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=flat-square&logo=json-web-tokens&logoColor=white)](https://jwt.io/)
  [![Vite](https://img.shields.io/badge/Vite-5.0-646CFF?style=flat-square&logo=vite&logoColor=white)](https://vitejs.dev/)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

  <p align="center">
    🌐 Live Demo  ·  📖 API Docs  ·  🐛 Report Bug  ·  ✨ Request Feature
  </p>
</div>

---

## 📸 Screenshots

**🏠 Home Page & Organization Discovery**
> *(Search across hospitals, banks, and public offices in a clean, glassmorphism UI)*

**📱 User Dashboard & Live Queue Tracker**
> *(Real-time wait times, current position, and dynamic progress bars)*

**📊 Super Admin Analytics Dashboard**
> *(Global metrics, organization health, and multi-tenant management)*

---

## ✨ Features

### 👤 Authentication & User Management
- ✅ **Global Single Sign-On**: One account works across all organizations.
- ✅ **JWT Authentication** with Bearer tokens.
- ✅ **BCrypt password hashing**.
- ✅ **Automated Email Verification** upon registration via SMTP.
- ✅ **Secure Password Reset** using time-limited tokens sent via email.
- ✅ **Role-Based Access Control (RBAC)** (USER, ORG_ADMIN, SUB_ADMIN, SUPER_ADMIN).

### 🏢 Organization & Multi-Tenant Management
- ✅ **Universal Booking Hub**: Support for Hospitals, Banks, Colleges, and Government Offices.
- ✅ **Dynamic Counter Creation**: Org Admins can spawn physical or virtual counters on demand.
- ✅ **Staff Management**: Assign doctors, teachers, or tellers to specific organizations.
- ✅ **Global Visibility Toggle**: Admins can suspend or activate organizations instantly.

### ⏱️ Digital Token & Queue Booking
- ✅ **Self-Service Booking**: Users can grab a token from any organization's counter.
- ✅ **Secure Payments**: Razorpay integration for premium/paid service counters.
- ✅ **Rate Limiting**: Strictly enforced limit of 2 active tokens per user per day.
- ✅ **Smart Wait-Time Prediction**: Dynamic algorithm calculates estimated turn time.
- ✅ **Status Lifecycle**: Pending → Serving → Completed (or Skipped/Cancelled).

### ⚡ Real-Time Features (WebSocket / STOMP)
- ✅ **Live Queue Pushes**: Instant updates to all waiting users when a token is processed.
- ✅ **"Call Next" Operations**: Counters trigger real-time UI refreshes globally (Synchronized with DB Transactions).
- ✅ **In-App Notifications**: Push notifications sent directly to the user's bell icon.

### 🤖 AI Assistant Integration
- ✅ **Q-Smart Assistant**: Built-in Google Gemini 1.5 Flash chatbot to guide users through the booking process and platform navigation.
- ✅ **Context-Aware**: Fallback offline responses configured for system resilience.

### 🔔 Integrated Notifications
- ✅ **Real-Time Bell Badge** with unread message counts.
- ✅ **Branded Email Dispatch**: "Q-Smart" branded emails powered by Gmail SMTP.
- ✅ **Event Triggers**: Registration, Password Resets, Token Updates.

---

## 🛠️ Tech Stack

### Backend
| Technology | Purpose |
| --- | --- |
| **Java 17** | Core language |
| **Spring Boot 3.3.5** | Application framework |
| **Spring Security** | Authentication & authorization |
| **Spring Data JPA** | ORM & database access |
| **Spring Mail** | SMTP email automation |
| **Spring WebSocket (STOMP)**| Real-time bi-directional communication |
| **JWT (jjwt 0.12.3)** | Stateless authentication |
| **Razorpay SDK** | Payment Gateway Integration |
| **Google Gemini API** | AI Chatbot logic |
| **BCrypt** | Password hashing |
| **Maven** | Build tool |

### Frontend
| Technology | Purpose |
| --- | --- |
| **React 18** | UI framework |
| **React Router DOM v6** | Client-side routing |
| **Axios** | HTTP client & interceptors |
| **Vanilla CSS** | Premium glassmorphism & responsive styling |
| **Lucide React** | Scalable vector iconography |
| **SockJS + STOMP.js** | WebSocket client |
| **Vite** | Lightning-fast build tool |

---

## 📁 Project Structure

```text
queueless-ai/
│
├── backend/
│   └── src/main/java/com/queueless/ai/
│       ├── config/           # SecurityConfig, WebSocketConfig, DataInitializer
│       ├── controller/       # Auth, Organization, Token, Analytics REST controllers
│       ├── dto/              # Request & Response Records
│       ├── entity/           # User, Organization, Token, Counter, Notification
│       ├── exception/        # GlobalExceptionHandler + custom runtime exceptions
│       ├── repository/       # Spring Data JPA interfaces
│       ├── security/         # JwtService, JwtAuthenticationFilter, UserPrincipal
│       ├── service/          # Business logic (TokenService, WaitTimePrediction)
│       └── websocket/        # QueueEventPublisher
│
├── frontend/
│   └── src/
│       ├── components/       # TopNav, StatusPill, EmptyState
│       ├── context/          # AuthContext (React Context API)
│       ├── pages/            # 
│       │   ├── admin/        # Dashboard, OrganizationManagement, QueueMonitoring
│       │   ├── user/         # BookTokenPage, QueueTrackingPage, NotificationsPage
│       │   └── auth/         # Login, Register, Forgot/Reset Password
│       ├── routes/           # ProtectedRoute with RBAC array support
│       ├── services/         # api.js (Axios instances), socket.js (STOMP)
│       └── styles.css        # Global CSS variables and utility classes
│
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

Make sure you have these installed:

| Tool | Version | Download |
| --- | --- | --- |
| **Java JDK** | 17+ | [adoptium.net](https://adoptium.net/) |
| **Maven** | 3.8+ | [maven.apache.org](https://maven.apache.org/) |
| **Node.js** | 18+ | [nodejs.org](https://nodejs.org/) |
| **MySQL** | 8.0+ | [mysql.com](https://www.mysql.com/) |
| **Git** | latest | [git-scm.com](https://git-scm.com/) |

### 1. Clone the Repository
```bash
git clone https://github.com/Sadikcserymec077/Queueless-Project.git
cd Queueless-Project
```

### 2. Set Up the Database
```sql
-- In MySQL client or Workbench
CREATE DATABASE queueless_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure the Backend
Navigate to the backend directory and open `application.yml` (`backend/src/main/resources/application.yml`).
Fill in your environment variables on your system, or update the file:

```yaml
DB_URL=jdbc:mysql://localhost:3306/queueless_ai
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# Generate a strong 64-character hex secret for JWT
JWT_SECRET=your_secure_jwt_secret_key

# Email configuration for SMTP notifications
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password   # Generate this via Google Security settings
```

> 💡 **Gmail App Password**: Go to Google Account → Security → 2-Step Verification → App passwords → Generate one for "QueueLess".

Run the backend:
```bash
cd backend
mvn spring-boot:run
```
*Backend starts at: `http://localhost:8081`*

### 4. Configure the Frontend
Navigate to the frontend directory:

```bash
cd frontend
npm install
npm run dev -- --port 5555
```
*Frontend starts at: `http://localhost:5555`*

### 5. Default Login Credentials
| Role | Email | Password |
| --- | --- | --- |
| **Super Admin** | `superadmin@queueless.ai` | `admin123` |
| **Org Admin** | `admin@citycare.example` | `admin123` |
| **Doctor** | `smith@citycare.example` | `user123` |
| **User** | `user@queueless.ai` | `user123` |

*(These accounts are automatically created by `DataInitializer.java` on the first startup).*

---

## ☁️ Deployment

### Backend → Render (Free)
1. Push backend to GitHub.
2. Go to render.com → New → Web Service.
3. Connect your GitHub repo.
4. Set Build Command: `mvn clean package -DskipTests`
5. Set Start Command: `java -jar target/queueless-ai-0.0.1-SNAPSHOT.jar`
6. Add all environment variables (`DB_URL`, `DB_USERNAME`, `JWT_SECRET`, etc.) in the dashboard.
7. Deploy! 🎉

### Database → Railway (Free $5 credit)
1. Go to railway.app → New Project → MySQL.
2. Copy the connection details.
3. Update `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in your Render environment variables.

### Frontend → Vercel or Netlify (Free)
1. Push frontend to GitHub.
2. Go to Vercel/Netlify → Add new site → Import from GitHub.
3. Set Build command: `npm run build`
4. Set Publish directory: `dist`
5. Add environment variable `VITE_API_URL` pointing to your live Render backend.
6. Deploy! 🎉

---

## 🔌 API Documentation
**Base URL**: `http://localhost:8081/api`

All authenticated endpoints require:
`Authorization: Bearer <access_token>`

### Quick Reference
| Module | Base Path | Key Endpoints |
| --- | --- | --- |
| **Auth** | `/auth` | `POST /register`, `POST /login`, `POST /verify-email`, `POST /reset-password` |
| **Organizations** | `/organizations` | `GET /`, `POST /`, `PUT /{id}/approve` |
| **Counters** | `/counters` | `POST /`, `GET /organization/{id}` |
| **Tokens** | `/tokens` | `POST /`, `GET /me/active`, `POST /counters/{id}/call-next` |
| **Notifications** | `/notifications` | `GET /`, `PATCH /{id}/read` |
| **Analytics** | `/analytics` | `GET /dashboard` |

---

## 🔐 Security
- All passwords hashed with **BCrypt**.
- **Stateless JWT tokens** passed strictly via HTTP headers.
- CORS configured to allow only trusted frontend origins.
- Input validation on all DTOs with `@Valid` Bean Validation.
- Organization data perfectly siloed using JPA repository constraints.
- `.gitignore` explicitly prevents environment files and secrets from being committed.

---

## 🤝 Contributing
Contributions are welcome!

```bash
# 1. Fork the repository
# 2. Create a feature branch
git checkout -b feat/your-feature-name

# 3. Commit your changes
git commit -m "feat(auth): add google oauth"

# 4. Push to your fork
git push origin feat/your-feature-name

# 5. Open a Pull Request
```

---

## 🙏 Acknowledgements
- **Spring Boot** — for the robust, enterprise-grade backend.
- **Lucide React** — for the beautiful, clean SVG icons.
- **Vite** — for the blazingly fast frontend compilation.

## 📄 License
This project is licensed under the MIT License — see the `LICENSE` file for details.

<div align="center">
  <i>Built with ❤️ by Mohammed Sadiq</i>
</div>
