import { Navigate, Route, Routes } from "react-router-dom";
import TopNav from "./components/TopNav.jsx";
import ProtectedRoute from "./routes/ProtectedRoute.jsx";
import HomePage from "./pages/HomePage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";
import VerifyEmailPage from "./pages/VerifyEmailPage.jsx";
import ForgotPasswordPage from "./pages/ForgotPasswordPage.jsx";
import ResetPasswordPage from "./pages/ResetPasswordPage.jsx";
import UserDashboard from "./pages/user/UserDashboard.jsx";
import BookTokenPage from "./pages/user/BookTokenPage.jsx";
import QueueTrackingPage from "./pages/user/QueueTrackingPage.jsx";
import NotificationsPage from "./pages/user/NotificationsPage.jsx";
import ProfilePage from "./pages/user/ProfilePage.jsx";
import AdminDashboard from "./pages/admin/AdminDashboard.jsx";
import OrganizationManagement from "./pages/admin/OrganizationManagement.jsx";
import CounterManagement from "./pages/admin/CounterManagement.jsx";
import QueueMonitoring from "./pages/admin/QueueMonitoring.jsx";
import AnalyticsDashboard from "./pages/admin/AnalyticsDashboard.jsx";
import UserManagement from "./pages/admin/UserManagement.jsx";
import AIChatbot from "./components/AIChatbot.jsx";

export default function App() {
  return (
    <>
      <TopNav />
      <main className="app-main">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          <Route element={<ProtectedRoute roles={["USER", "STAFF", "DOCTOR", "TEACHER", "STUDENT", "PATIENT", "SUB_ADMIN", "ORG_ADMIN", "SUPER_ADMIN"]} />}>
            <Route path="/user/dashboard" element={<UserDashboard />} />
            <Route path="/user/book" element={<BookTokenPage />} />
            <Route path="/user/track" element={<QueueTrackingPage />} />
            <Route path="/user/notifications" element={<NotificationsPage />} />
            <Route path="/user/profile" element={<ProfilePage />} />
          </Route>

          <Route element={<ProtectedRoute roles={["SUPER_ADMIN"]} />}>
            <Route path="/admin/organizations" element={<OrganizationManagement />} />
          </Route>

          <Route element={<ProtectedRoute roles={["SUPER_ADMIN", "ORG_ADMIN", "SUB_ADMIN"]} />}>
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
            <Route path="/admin/users" element={<UserManagement />} />
            <Route path="/admin/counters" element={<CounterManagement />} />
            <Route path="/admin/queue" element={<QueueMonitoring />} />
            <Route path="/admin/analytics" element={<AnalyticsDashboard />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <AIChatbot />
      </main>
    </>
  );
}
