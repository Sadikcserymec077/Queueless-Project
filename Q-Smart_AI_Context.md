# Q-Smart (Queueless AI) - Comprehensive Project Context

This document is designed to give any AI agent or developer a complete, from-scratch understanding of the **Q-Smart** platform (also known as Queueless-Project). Use this as your root context when discussing architecture, adding new features, or debugging.

## 1. Project Identity & Vision
**Q-Smart** is a production-ready, full-stack Queue and Appointment Management Platform. 
**Goal:** Eliminate physical waiting lines. It acts as a universal booking hub where users can digitally book tokens for various organizations (Hospitals, Banks, Government Offices, Colleges), track their live queue position, and arrive exactly when it is their turn.

## 2. Technology Stack
*   **Backend:** Java 17, Spring Boot 3.3.5, Spring Security, Spring Data JPA, Spring Mail.
*   **Real-time Communication:** Spring WebSocket with STOMP protocol.
*   **Frontend:** React 18, Vite, React Router DOM v6, Axios, Vanilla CSS (Premium glassmorphism UI), Lucide React.
*   **Database:** MySQL 8.0 (Local) / PostgreSQL (Production).
*   **Authentication:** Stateless JWT (JSON Web Tokens), BCrypt Password Hashing, Google OAuth2 Integration.
*   **Third-Party Services:** Google Gemini 1.5 Flash (AI Assistant), Razorpay SDK (Payments), Gmail SMTP (Notifications).

## 3. Core Features Breakdown

### A. Multi-Tenant Organization Management
*   **Organization Hub:** Supports diverse entities like `HOSPITAL`, `BANK`, `OFFICE`, and `COLLEGE`.
*   **Administrative Gating:** Organizations apply to join. A `SUPER_ADMIN` must approve them (`OrganizationStatus`: PENDING -> APPROVED) before they go live.
*   **Dynamic Counters:** Once approved, `ORG_ADMIN`s can dynamically spawn physical or virtual counters and assign staff (`SUB_ADMIN`) to operate them.

### B. Authentication & RBAC (Role-Based Access Control)
*   Roles: `SUPER_ADMIN`, `ORG_ADMIN`, `SUB_ADMIN`, and `USER`.
*   Supports standard Email/Password authentication with SMTP-based email verification and password reset flows.
*   Supports **Google Sign-In / OAuth2** for seamless onboarding.

### C. Digital Token & Queue Lifecycle
*   **Self-Service Booking:** Users select an organization, choose a counter, and book a token.
*   **Smart Limitations:** Strictly rate-limited (e.g., 2 active tokens per user per day) to prevent spam.
*   **Wait-Time Prediction:** Backend algorithms calculate estimated wait times based on live queue flow and historical processing times.
*   **Token Statuses:** Tokens move sequentially: `PENDING` -> `SERVING` -> `COMPLETED` (or `CANCELLED`).

### D. Real-Time Synchronization (WebSockets)
*   Built completely on STOMP WebSockets.
*   When a `SUB_ADMIN` clicks **"Call Next"**, the backend executes DB transactions and immediately publishes a message to a WebSocket topic.
*   All connected React clients receive the push and update their UI instantly without polling.

### E. Integrated AI & Notifications
*   **Q-Smart Assistant:** A built-in chatbot powered by Google Gemini 1.5 Flash, contextually aware of the user's navigation and queue rules to assist them.
*   **In-App & Email Notifications:** Real-time bell badge updates and branded email dispatches for critical events (booking confirmation, nearing turn).

## 4. Database Schema & Entities
*   **User:** Manages auth credentials, roles, OAuth mapping, and refresh tokens.
*   **Organization:** Has name, address, `OrganizationType`, and `OrganizationStatus`. Contains multiple counters.
*   **Counter:** A desk processing tokens. Has `CounterStatus` and is assigned to a `User` with `SUB_ADMIN` role.
*   **Token:** The core entity linking a `User` to a `Counter`. Holds `TokenStatus`, position in line, and estimated processing times.
*   **Notification:** Stores user-specific alerts and unread status.
*   **Payment & QueueAnalytics:** Tracks Razorpay transactions (for premium queues) and global platform metrics.

## 5. Typical Workflows to Understand

### The Token Processing Flow
1. **User** browses available organizations -> picks a counter -> clicks "Book".
2. **Backend** verifies rate limits -> creates `Token` -> assigns queue number.
3. **Frontend** listens to WebSocket channel `/topic/queue/{counterId}`.
4. **Sub-Admin** at the counter clicks "Call Next".
5. **Backend** marks current token as `COMPLETED`, next token as `SERVING` -> Publishes STOMP message.
6. **Frontend** updates the UI instantly for everyone waiting.

### Authentication Flow
*   **Standard Login:** Frontend sends credentials -> Backend validates -> Returns JWT -> Frontend stores in localStorage/context -> Attaches as `Bearer` token in Axios interceptor for subsequent requests.
*   **OAuth Flow:** Frontend handles Google Login -> sends Google token to backend -> backend validates with Google -> issues internal JWT.

## 6. How to Run Locally
1. **Database:** Ensure MySQL is running locally. Create database `queueless_ai`.
2. **Backend setup (`backend/src/main/resources/application.yml`):**
    *   Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
    *   Set `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `GEMINI_API_KEY`.
3. **Run Backend:** `cd backend && mvn spring-boot:run` (Port 8081).
4. **Run Frontend:** `cd frontend && npm install && npm run dev -- --port 5555` (Port 5555).
    *   Frontend needs `VITE_API_URL` pointing to backend.

## 7. Recent Development History & Known Context
*(Context on what was recently built or fixed in the project)*
*   **PostgreSQL Migration & OAuth Fixes:** Recently migrated database logic to handle PostgreSQL constraints, specifically stabilizing Refresh Token tracking during the Google OAuth flow.
*   **Organization Onboarding Design:** Built secure workflows for the `OrganizationStatus` lifecycle (Pending -> Approved).
*   **Architecture Analysis:** Ongoing work to verify Redis integration (if any) and optimize caching/queue states.

---
**Agent Instructions:** When the developer asks you to add a feature, refactor code, or fix a bug, refer to the entity relationships, tech stack, and workflows described above to ensure your code aligns perfectly with the Q-Smart architecture.
