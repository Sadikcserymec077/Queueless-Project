import { Bell, CalendarPlus, Clock, ListChecks } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import EmptyState from "../../components/EmptyState.jsx";
import StatsCard from "../../components/StatsCard.jsx";
import TokenCard from "../../components/TokenCard.jsx";
import { tokensApi } from "../../services/api.js";
import { apiError, formatDate } from "../../utils/format.js";

export default function UserDashboard() {
  const [active, setActive] = useState(null);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        const [activeToken, historyPage] = await Promise.all([tokensApi.active(), tokensApi.history({ size: 5 })]);
        setActive(activeToken);
        setHistory(historyPage.content);
      } catch (err) {
        setError(apiError(err));
      }
    }
    load();
  }, []);

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">User workspace</p>
          <h1>Dashboard</h1>
        </div>
        <Link className="primary-action" to="/user/book"><CalendarPlus size={18} /> Book token</Link>
      </div>

      {error ? <div className="alert alert-warning">{error}</div> : null}

      <div className="stats-grid">
        <StatsCard icon={Clock} label="Active token" value={active?.tokenNumber || "None"} />
        <StatsCard icon={ListChecks} label="Recent bookings" value={history.length} tone="success" />
        <StatsCard icon={Bell} label="Queue status" value={active?.status || "Idle"} tone="warning" />
      </div>

      {active ? (
        <TokenCard token={active} />
      ) : (
        <EmptyState title="No active token" text="Choose an organization and counter to book your next virtual token." action={<Link className="secondary-action" to="/user/book">Find counters</Link>} />
      )}

      <section className="data-section">
        <h2>Recent history</h2>
        <div className="table-responsive">
          <table>
            <thead><tr><th>Token</th><th>Organization</th><th>Status</th><th>Booked</th></tr></thead>
            <tbody>
              {history.map((token) => (
                <tr key={token.id}>
                  <td>{token.tokenNumber}</td>
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
