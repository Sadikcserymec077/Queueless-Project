import { CheckCircle2, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { authApi } from "../services/api.js";

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const [status, setStatus] = useState("loading"); // loading, success, error
  const [message, setMessage] = useState("Verifying your email address...");

  useEffect(() => {
    if (!token) {
      setStatus("error");
      setMessage("No verification token found.");
      return;
    }

    const verify = async () => {
      try {
        await authApi.verifyEmail(token);
        setStatus("success");
        setMessage("Your email has been verified successfully! You can now log in.");
      } catch (err) {
        setStatus("error");
        setMessage(err.response?.data?.message || "Failed to verify email. The link may have expired.");
      }
    };

    verify();
  }, [token]);

  return (
    <section className="auth-layout">
      <div className="auth-card" style={{ textAlign: "center", padding: "2.5rem 1.5rem" }}>
        {status === "loading" && <div className="spinner" style={{ margin: "0 auto 1rem" }}></div>}
        {status === "success" && <CheckCircle2 size={48} color="var(--success)" style={{ margin: "0 auto 1rem" }} />}
        {status === "error" && <XCircle size={48} color="var(--danger)" style={{ margin: "0 auto 1rem" }} />}
        
        <h2>{status === "loading" ? "Verifying..." : status === "success" ? "Verified!" : "Verification Failed"}</h2>
        <p style={{ color: "var(--muted)", margin: "0.5rem 0 1.5rem" }}>{message}</p>
        
        {status !== "loading" && (
          <Link to="/login" className="primary-action" style={{ display: "inline-flex", padding: "0.6rem 1.5rem" }}>
            Go to Login
          </Link>
        )}
      </div>
    </section>
  );
}
