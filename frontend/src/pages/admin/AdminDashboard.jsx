import { Building2, CheckCircle2, Clock, UsersRound, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import StatsCard from "../../components/StatsCard.jsx";
import { useAuth } from "../../context/AuthContext.jsx";
import { analyticsApi } from "../../services/api.js";
import { apiError } from "../../utils/format.js";

export default function AdminDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        setStats(await analyticsApi.dashboard());
      } catch (err) {
        setError(apiError(err));
      }
    }
    load();
  }, []);

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div><p className="eyebrow">{user?.role === 'SUPER_ADMIN' ? 'Platform Operations' : 'Organization Operations'}</p><h1>{user?.role === 'SUPER_ADMIN' ? 'Super Admin Dashboard' : 'Organization Dashboard'}</h1></div>
        <Link className="primary-action" to="/admin/queue">Monitor queue</Link>
      </div>
      {error ? <div className="alert alert-warning">{error}</div> : null}
      <div className="stats-grid">
        <StatsCard icon={UsersRound} label="Total users" value={stats?.totalUsers ?? 0} />
        <StatsCard icon={Building2} label="Active queues" value={stats?.activeQueues ?? 0} tone="warning" />
        <StatsCard icon={Clock} label="Total tokens" value={stats?.totalTokens ?? 0} tone="info" />
        <StatsCard icon={CheckCircle2} label="Completed" value={stats?.completedServices ?? 0} tone="success" />
        <StatsCard icon={XCircle} label="Cancelled" value={stats?.cancelledTokens ?? 0} tone="danger" />
      </div>
      <div className="admin-shortcuts">
        {user?.role === 'SUPER_ADMIN' && <Link to="/admin/organizations">Manage organizations</Link>}
        <Link to="/admin/users">Manage users</Link>
        <Link to="/admin/counters">Manage counters</Link>
        <Link to="/admin/analytics">View analytics</Link>
      </div>
    </section>
  );
}
