import { UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "../../context/AuthContext.jsx";
import { tokensApi } from "../../services/api.js";
import { apiError, formatDate } from "../../utils/format.js";

export default function ProfilePage() {
  const { user } = useAuth();
  const [history, setHistory] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        const page = await tokensApi.history({ size: 20 });
        setHistory(page.content);
      } catch (err) {
        setError(apiError(err));
      }
    }
    load();
  }, []);

  return (
    <section className="page-stack">
      <div className="profile-band" style={{ flexWrap: "wrap", gap: "2rem" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
          <UserRound size={32} />
          <div>
            <p className="eyebrow">{user.role}</p>
            <h1>{user.name}</h1>
            <p>{user.email}</p>
          </div>
        </div>
        
        <form onSubmit={async (e) => {
          e.preventDefault();
          try {
            const formData = new FormData(e.target);
            await import("../../services/api.js").then(m => m.authApi.updateProfile({ phone: formData.get("phone") }));
            alert("Phone number updated successfully! Please re-login to see changes globally.");
          } catch(err) {
            alert(apiError(err));
          }
        }} style={{ display: "flex", gap: "0.5rem", alignItems: "flex-end" }}>
          <label style={{ margin: 0 }}>
            Phone Number
            <input name="phone" type="tel" defaultValue={user.phone || ""} placeholder="Add phone number" />
          </label>
          <button type="submit" className="primary-action" style={{ padding: "0.6rem 1rem" }}>Save</button>
        </form>
      </div>
      {error ? <div className="alert alert-warning">{error}</div> : null}
      <section className="data-section">
        <h2>Booking history</h2>
        <div className="table-responsive">
          <table>
            <thead><tr><th>Token</th><th>Counter</th><th>Organization</th><th>Status</th><th>Booked</th></tr></thead>
            <tbody>
              {history.map((token) => (
                <tr key={token.id}>
                  <td>{token.tokenNumber}</td>
                  <td>{token.counterName}</td>
                  <td>{token.organizationName}</td>
                  <td>{token.status}</td>
                  <td>{formatDate(token.bookingTime)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}
