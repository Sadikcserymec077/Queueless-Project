import { UserPlus, Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { apiError } from "../utils/format.js";

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await register(form);
      navigate("/dashboard");
    } catch (err) {
      setError(apiError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="auth-layout">
      <form className="auth-card" onSubmit={submit}>
        <p className="eyebrow">Virtual token access</p>
        <h1>Create account</h1>
        {error ? <div className="alert alert-danger">{error}</div> : null}
        <label>Name<input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
        <label>Email<input type="email" required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        <label>Phone Number<input required type="tel" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} placeholder="e.g. +1234567890" /></label>
        <label>Password
          <div style={{ position: "relative" }}>
            <input type={showPassword ? "text" : "password"} minLength={6} required value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} style={{ width: "100%", paddingRight: "40px" }} />
            <button type="button" onClick={() => setShowPassword(!showPassword)} style={{ position: "absolute", right: "10px", top: "50%", transform: "translateY(-50%)", background: "transparent", border: "none", cursor: "pointer", color: "inherit", opacity: 0.7, padding: 0, display: "flex" }}>
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </label>
        <button className="primary-action w-100" disabled={busy} type="submit"><UserPlus size={18} />{busy ? "Creating" : "Register"}</button>
        <p className="auth-note">Already registered? <Link to="/login">Login</Link></p>
      </form>
    </section>
  );
}
