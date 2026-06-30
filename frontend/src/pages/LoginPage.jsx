import { LogIn, Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { apiError } from "../utils/format.js";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const user = await login(form);
      const fallback = (user.role === "SUPER_ADMIN" || user.role === "ORG_ADMIN" || user.role === "SUB_ADMIN") ? "/admin/dashboard" : "/user/dashboard";
      navigate(location.state?.from?.pathname || fallback, { replace: true });
    } catch (err) {
      setError(apiError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="auth-layout">
      <form className="auth-card" onSubmit={submit}>
        <p className="eyebrow">Welcome back</p>
        <h1>Login</h1>
        {error ? <div className="alert alert-danger">{error}</div> : null}
        <label>Email<input type="email" required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        <label>Password
          <div style={{ position: "relative" }}>
            <input type={showPassword ? "text" : "password"} required value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} style={{ width: "100%", paddingRight: "40px" }} />
            <button type="button" onClick={() => setShowPassword(!showPassword)} style={{ position: "absolute", right: "10px", top: "50%", transform: "translateY(-50%)", background: "transparent", border: "none", cursor: "pointer", color: "inherit", opacity: 0.7, padding: 0, display: "flex" }}>
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </label>
        <div style={{ textAlign: "right", marginTop: "-0.5rem", marginBottom: "0.5rem" }}>
          <Link to="/forgot-password" style={{ fontSize: "0.85rem", color: "var(--primary)", fontWeight: "600" }}>Forgot password?</Link>
        </div>
        <button className="primary-action w-100" disabled={busy} type="submit"><LogIn size={18} />{busy ? "Signing in" : "Login"}</button>

        <div style={{ display: "flex", alignItems: "center", margin: "1.5rem 0", color: "#9ca3af" }}>
          <div style={{ flex: 1, height: "1px", backgroundColor: "#e5e7eb" }}></div>
          <span style={{ padding: "0 0.75rem", fontSize: "0.875rem" }}>or</span>
          <div style={{ flex: 1, height: "1px", backgroundColor: "#e5e7eb" }}></div>
        </div>
        
        <button 
          type="button"
          onClick={() => setError("Google OAuth is currently being configured for production. Please use standard email login for now.")}
          style={{ width: "100%", padding: "0.75rem", display: "flex", alignItems: "center", justifyContent: "center", gap: "0.75rem", backgroundColor: "white", border: "1px solid #d1d5db", borderRadius: "8px", color: "#374151", fontWeight: "600", cursor: "pointer", marginBottom: "1.5rem", transition: "background-color 0.2s" }}
          onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#f9fafb'}
          onMouseOut={(e) => e.currentTarget.style.backgroundColor = 'white'}
        >
          <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" style={{ width: "20px", height: "20px" }} />
          Login with Google
        </button>

        <p className="auth-note">New here? <Link to="/register">Create an account</Link></p>
      </form>
    </section>
  );
}
