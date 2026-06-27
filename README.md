<div align="center">
  
  # 🚀 QueueLess AI
  
  **Next-Generation Multi-Tenant Queue & Appointment Management Platform**

  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![React](https://img.shields.io/badge/React-18.3-61DAFB?style=flat-square&logo=react&logoColor=black)](https://reactjs.org/)
  [![Vite](https://img.shields.io/badge/Vite-5.0-646CFF?style=flat-square&logo=vite&logoColor=white)](https://vitejs.dev/)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

  <p align="center">
    QueueLess AI eliminates physical waiting lines by offering real-time, digital token booking across Hospitals, Banks, Colleges, and Government Offices.
  </p>
</div>

---

## ✨ Key Features

- 🏢 **Multi-Tenant Architecture**: A single global account gives users access to book tokens across any registered organization.
- ⚡ **Real-Time WebSockets**: Live queue updates, token status changes, and instant notifications pushed directly to the frontend.
- 🔐 **Robust Security**: JWT-based stateless authentication, BCrypt password hashing, and role-based access control (RBAC).
- ✉️ **Integrated SMTP Notifications**: Automated email verification, password reset flows, and branded welcomes.
- 📊 **Advanced Analytics Dashboard**: Wait-time predictions, historical queue data, and operational metrics for administrators.
- 📱 **Mobile-First UI**: Responsive, glassmorphism-inspired React frontend built for optimal mobile and desktop experiences.
- 🛡️ **Rate Limiting**: Enforced booking limits (max 2 daily tokens) via UTC window validation to prevent spam.

## 🛠️ Technology Stack

| Component | Technology |
| --- | --- |
| **Frontend** | React 18, Vite, React Router DOM, Axios, Vanilla CSS (Glassmorphism UI) |
| **Backend** | Java 17, Spring Boot 3.3.5, Spring Security, Spring Data JPA |
| **Database** | MySQL 8.0, Hibernate ORM |
| **Real-Time** | Spring WebSocket (STOMP), SockJS |
| **Authentication** | JSON Web Tokens (JWT) |

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Node.js 18+**
- **MySQL 8.0**
- **Maven 3.8+**

### Local Setup

**1. Clone the repository**
```bash
git clone https://github.com/Sadikcserymec077/Queueless-Project.git
cd Queueless-Project
```

**2. Database Configuration**
Create a MySQL database named `queueless_ai`.

**3. Backend Configuration (`backend/src/main/resources/application.yml`)**
Set your environment variables or update the file with your local credentials:
```yaml
DB_USERNAME: root
DB_PASSWORD: your_password
MAIL_PASSWORD: your_google_app_password
JWT_SECRET: your_secure_jwt_secret_key
```

**4. Start the Backend**
```bash
cd backend
mvn spring-boot:run
```
*The API will start at `http://localhost:8081`.*

**5. Start the Frontend**
```bash
cd frontend
npm install
npm run dev -- --port 5555
```
*The web app will start at `http://localhost:5555`.*

## 🧑‍💻 Usage & Roles

The system automatically seeds a set of demo users and organizations if `app.seed.enabled` is true.

- **Super Admin** (`superadmin@queueless.ai`): Can create organizations, view global analytics, and manage ORG_ADMINs.
- **Org Admin** (`admin@citycare.example`): Can add staff/doctors, create counters, and monitor queues for their specific organization.
- **User** (`user@queueless.ai`): Can browse organizations, book digital tokens, and track real-time queue position.

## 🔒 Security Architecture

- **Stateless Sessions**: JWT tokens are passed via `Bearer` headers.
- **Method-Level Security**: `@PreAuthorize` restricts endpoint execution to specific roles (`hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')`).
- **Data Isolation**: Organizations can only view and mutate their own counters and tokens. Global users can interact across tenants.

## 🤝 Contributing

Contributions are always welcome! Please follow standard GitHub flow:
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.

---
<div align="center">
  <i>Built with ❤️ for a Queue-less world.</i>
</div>
