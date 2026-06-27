import { ArrowRight, Building2, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import EmptyState from "../components/EmptyState.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { organizationsApi } from "../services/api.js";
import { apiError } from "../utils/format.js";

const types = ["HOSPITAL", "BANK", "COLLEGE", "GOVERNMENT_OFFICE", "SERVICE_CENTER"];

export default function HomePage() {
  const { user } = useAuth();
  const [q, setQ] = useState("");
  const [type, setType] = useState("");
  const [organizations, setOrganizations] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    const handle = setTimeout(async () => {
      try {
        setError("");
        const page = await organizationsApi.search({ q: q || undefined, type: type || undefined, size: 9 });
        setOrganizations(page.content);
      } catch (err) {
        setError(apiError(err));
      }
    }, 250);
    return () => clearTimeout(handle);
  }, [q, type]);

  return (
    <section className="page-stack">
      <div className="home-hero">
        <div>
          <p className="eyebrow">Smart Queue and Appointment Management</p>
          <h1>QueueLess AI</h1>
          <p className="lead-text">Book a virtual token, watch live queue movement, and arrive when your turn is close.</p>
        </div>
        <div className="hero-actions">
          <Link className="primary-action" to={(user?.role === "SUPER_ADMIN" || user?.role === "ORG_ADMIN" || user?.role === "SUB_ADMIN") ? "/admin/dashboard" : user ? "/user/book" : "/register"}>
            {user ? "Open dashboard" : "Start booking"}
            <ArrowRight size={18} />
          </Link>
          {!user ? <Link className="secondary-action" to="/login">Login</Link> : null}
        </div>
      </div>

      <div className="toolbar">
        <label className="search-box">
          <Search size={18} />
          <input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search organizations" />
        </label>
        <select value={type} onChange={(event) => setType(event.target.value)} aria-label="Organization type">
          <option value="">All types</option>
          {types.map((item) => <option key={item} value={item}>{item.replaceAll("_", " ")}</option>)}
        </select>
      </div>

      {error ? <div className="alert alert-warning">{error}</div> : null}
      {organizations.length === 0 && !error ? (
        <EmptyState title="No organizations found" text="Try another search term or type filter." />
      ) : (
        <div className="entity-grid">
          {organizations.map((organization) => (
            <article className="entity-card" key={organization.id}>
              <div className="entity-icon"><Building2 size={22} /></div>
              <p className="eyebrow">{organization.type.replaceAll("_", " ")}</p>
              <h2>{organization.name}</h2>
              <p>{organization.address}</p>
              <dl className="compact-metrics">
                <div><dt>Active counters</dt><dd>{organization.activeCounters}</dd></div>
                <div><dt>Active queue</dt><dd>{organization.activeTokens}</dd></div>
              </dl>
              <Link className="inline-link" to={user ? `/user/book?orgId=${organization.id}` : "/login"}>Book token <ArrowRight size={16} /></Link>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
