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
  const [tab, setTab] = useState("live");

  useEffect(() => {
    async function load() {
      try {
        // Fetch more history to ensure we get upcoming ones too
        const [activeToken, historyPage] = await Promise.all([tokensApi.active(), tokensApi.history({ size: 50 })]);
        setActive(activeToken);
        setHistory(historyPage.content);
      } catch (err) {
        setError(apiError(err));
      }
    }
    load();
  }, []);

  const todayStr = new Date().toISOString().split("T")[0];

  const liveTokens = history.filter(t => 
    (t.status === "WAITING" || t.status === "CALLED") && 
    (!t.scheduledDate || t.scheduledDate === todayStr)
  );

  const upcomingTokens = history.filter(t => 
    t.status === "WAITING" && 
    t.scheduledDate && 
    t.scheduledDate > todayStr
  );

  const pastTokens = history.filter(t => 
    t.status === "COMPLETED" || t.status === "CANCELLED" || t.status === "SKIPPED"
  );

  const displayTokens = tab === "live" ? liveTokens : upcomingTokens;

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
        <StatsCard icon={Clock} label="Live token" value={liveTokens.length > 0 ? liveTokens[0].tokenNumber : "None"} />
        <StatsCard icon={ListChecks} label="Upcoming" value={upcomingTokens.length} tone="success" />
        <StatsCard icon={Bell} label="Past bookings" value={pastTokens.length} tone="warning" />
      </div>

      <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--border)', marginBottom: '1.5rem' }}>
        <button 
          style={{ padding: '0.75rem 1rem', background: 'none', border: 'none', borderBottom: tab === 'live' ? '2px solid var(--primary)' : 'none', color: tab === 'live' ? 'var(--text-primary)' : 'var(--text-muted)', fontWeight: tab === 'live' ? 'bold' : 'normal', cursor: 'pointer' }}
          onClick={() => setTab("live")}
        >
          Live Now
        </button>
        <button 
          style={{ padding: '0.75rem 1rem', background: 'none', border: 'none', borderBottom: tab === 'upcoming' ? '2px solid var(--primary)' : 'none', color: tab === 'upcoming' ? 'var(--text-primary)' : 'var(--text-muted)', fontWeight: tab === 'upcoming' ? 'bold' : 'normal', cursor: 'pointer' }}
          onClick={() => setTab("upcoming")}
        >
          Upcoming Events
        </button>
      </div>

      {displayTokens.length > 0 ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', marginBottom: '2rem' }}>
          {displayTokens.map(token => <TokenCard key={token.id} token={token} compact={tab === 'upcoming'} />)}
        </div>
      ) : (
        <EmptyState 
          title={tab === "live" ? "No live tokens" : "No upcoming events"} 
          text={tab === "live" ? "You don't have any active tokens for today." : "You have no future scheduled bookings."} 
          action={<Link className="secondary-action" to="/user/book">Find counters</Link>} 
        />
      )}

      <section className="data-section">
        <h2>Recent history</h2>
        <div className="table-responsive">
          <table>
            <thead><tr><th>Token</th><th>Organization</th><th>Status</th><th>Date</th></tr></thead>
            <tbody>
              {pastTokens.slice(0, 5).map((token) => (
                <tr key={token.id}>
                  <td>{token.tokenNumber}</td>
                  <td>{token.organizationName}</td>
                  <td>{token.status}</td>
                  <td>{token.scheduledDate || formatDate(token.bookingTime)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </section>
  );
}
