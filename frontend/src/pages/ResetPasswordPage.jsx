import { KeyRound, Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { authApi } from "../services/api.js";
import { apiError } from "../utils/format.js";

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const navigate = useNavigate();
  
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [busy, setBusy] = useState(false);

  if (!token) {
    return (
      <section className="auth-layout">
        <div className="auth-card" style={{ textAlign: "center" }}>
          <h2>Invalid Link</h2>
          <p>No reset token found in the URL. Please request a new password reset link.</p>
          <Link to="/forgot-password" className="primary-action">Request Reset Link</Link>
        </div>
      </section>
    );
  }

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await authApi.resetPassword({ token, newPassword: password });
      setSuccess(true);
      setTimeout(() => navigate("/login"), 3000);
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
        <h1>Set New Password</h1>
        
        {error ? <div className="alert alert-danger">{error}</div> : null}
        {success ? (
          <div className="alert alert-success">Password reset successfully! Redirecting to login...</div>
        ) : (
          <>
            <label>New Password
              <div style={{ position: "relative" }}>
                <input 
                  type={showPassword ? "text" : "password"} 
                  minLength={6} 
                  required 
                  value={password} 
                  onChange={(e) => setPassword(e.target.value)} 
                  style={{ width: "100%", paddingRight: "40px" }} 
                />
                <button 
                  type="button" 
                  onClick={() => setShowPassword(!showPassword)} 
                  style={{ position: "absolute", right: "10px", top: "50%", transform: "translateY(-50%)", background: "transparent", border: "none", cursor: "pointer", color: "inherit", opacity: 0.7, padding: 0, display: "flex" }}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </label>
            
            <button className="primary-action w-100" disabled={busy} type="submit">
              <KeyRound size={18} />
              {busy ? "Saving..." : "Save New Password"}
            </button>
          </>
        )}
      </form>
    </section>
  );
}
