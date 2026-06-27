import { Mail } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { authApi } from "../services/api.js";
import { apiError } from "../utils/format.js";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    setSuccess(false);
    try {
      await authApi.forgotPassword({ email });
      setSuccess(true);
      setEmail("");
    } catch (err) {
      setError(apiError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="auth-layout">
      <form className="auth-card" onSubmit={submit}>
        <p className="eyebrow">Recovery</p>
        <h1>Reset Password</h1>
        <p style={{ color: "var(--muted)", margin: "0 0 1rem", fontSize: "0.9rem" }}>
          Enter your email address and we'll send you a link to reset your password.
        </p>

        {error ? <div className="alert alert-danger">{error}</div> : null}
        {success ? <div className="alert alert-success">If an account exists with that email, a password reset link has been sent.</div> : null}

        <label>Email
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@example.com" />
        </label>
        
        <button className="primary-action w-100" disabled={busy} type="submit">
          <Mail size={18} />
          {busy ? "Sending..." : "Send Reset Link"}
        </button>
        
        <p className="auth-note">
          Remember your password? <Link to="/login">Back to Login</Link>
        </p>
      </form>
    </section>
  );
}
