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
      <div className="profile-band">
        <UserRound size={32} />
        <div>
          <p className="eyebrow">{user.role}</p>
          <h1>{user.name}</h1>
          <p>{user.email}</p>
        </div>
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
