import { CalendarPlus, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import StatusPill from "../../components/StatusPill.jsx";
import TokenCard from "../../components/TokenCard.jsx";
import { countersApi, organizationsApi, tokensApi } from "../../services/api.js";
import { apiError } from "../../utils/format.js";

export default function BookTokenPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialOrgId = searchParams.get("orgId") || "";

  const [q, setQ] = useState("");
  const [organizations, setOrganizations] = useState([]);
  const [organizationId, setOrganizationId] = useState(initialOrgId);
  const [counters, setCounters] = useState([]);
  const [booked, setBooked] = useState(null);
  const [error, setError] = useState("");
  const [busyCounter, setBusyCounter] = useState(null);

  const selectedOrg = organizations.find(o => String(o.id) === organizationId);

  useEffect(() => {
    async function loadOrganizations() {
      try {
        const page = await organizationsApi.search({ q: q || undefined, size: 20 });
        setOrganizations(page.content);
        if (!organizationId && page.content.length) {
          setOrganizationId(String(page.content[0].id));
        }
      } catch (err) {
        setError(apiError(err));
      }
    }
    loadOrganizations();
  }, [q]);

  useEffect(() => {
    if (!organizationId) return;
    async function loadCounters() {
      try {
        setCounters(await countersApi.byOrganization(organizationId));
      } catch (err) {
        setError(apiError(err));
      }
    }
    loadCounters();
  }, [organizationId]);

  const book = async (counterId) => {
    setBusyCounter(counterId);
    setError("");
    try {
      setBooked(await tokensApi.book(counterId));
    } catch (err) {
      setError(apiError(err));
    } finally {
      setBusyCounter(null);
    }
  };

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Virtual queue</p>
          <h1>Book token</h1>
        </div>
      </div>

      {error ? <div className="alert alert-warning">{error}</div> : null}
      {booked ? (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: 'var(--bg-card)', padding: '2rem', borderRadius: '12px', maxWidth: '400px', width: '90%', textAlign: 'center', boxShadow: '0 4px 20px rgba(0,0,0,0.1)' }}>
            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
              <div style={{ width: '60px', height: '60px', borderRadius: '50%', backgroundColor: 'var(--success-bg, #e6f6ec)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--success-text, #0f5132)' }}>
                <CalendarPlus size={32} />
              </div>
            </div>
            <h2 style={{ marginTop: 0, marginBottom: '0.5rem' }}>Booking Confirmed!</h2>
            <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>Your Token Number is <strong style={{color: 'var(--text-primary)', fontSize: '1.1rem'}}>{booked.tokenNumber}</strong>.</p>
            <button className="primary-action" onClick={() => navigate("/user/track")} style={{ width: '100%', padding: '0.75rem', justifyContent: 'center' }}>
              Acknowledge & View QR
            </button>
          </div>
        </div>
      ) : null}

      <div className="toolbar">
        <label className="search-box"><Search size={18} /><input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search organization" /></label>
        <select value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} aria-label="Organization">
          {organizations.map((organization) => <option key={organization.id} value={organization.id}>{organization.name}</option>)}
        </select>
      </div>

      {selectedOrg && (
        <div style={{ backgroundColor: "var(--bg-card, #fff)", border: "1px solid var(--border)", borderRadius: "8px", padding: "1.5rem", marginBottom: "2rem" }}>
          <h2 style={{ marginTop: 0, marginBottom: "0.5rem" }}>{selectedOrg.name}</h2>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "1rem", color: "var(--text-muted)", fontSize: "0.9rem" }}>
            <div>
              <strong style={{ color: "var(--text-primary)", display: "block", marginBottom: "0.25rem" }}>Address</strong>
              {selectedOrg.address || "Not provided"}
            </div>
            <div>
              <strong style={{ color: "var(--text-primary)", display: "block", marginBottom: "0.25rem" }}>Working Hours</strong>
              {selectedOrg.workingHours || "Not provided"}
            </div>
            {selectedOrg.holidays && (
              <div>
                <strong style={{ color: "var(--text-primary)", display: "block", marginBottom: "0.25rem" }}>Closed On</strong>
                {selectedOrg.holidays}
              </div>
            )}
            <div>
              <strong style={{ color: "var(--text-primary)", display: "block", marginBottom: "0.25rem" }}>Contact</strong>
              {selectedOrg.contactNumber || "Not provided"}
            </div>
          </div>
        </div>
      )}

      <div className="entity-grid">
        {counters.map((counter) => (
          <article className="entity-card" key={counter.id}>
            <p className="eyebrow">{counter.organizationName}</p>
            <h2>{counter.counterName}</h2>
            <p>{counter.serviceType}</p>
            <dl className="compact-metrics">
              <div><dt>Waiting</dt><dd>{counter.waitingTokens}</dd></div>
              <div><dt>Current</dt><dd>{counter.currentToken || "None"}</dd></div>
            </dl>
            <div className="entity-actions">
              <StatusPill value={counter.status} />
              <button className="primary-action" disabled={counter.status !== "ACTIVE" || busyCounter === counter.id} type="button" onClick={() => book(counter.id)}>
                <CalendarPlus size={18} />
                {busyCounter === counter.id ? "Booking" : "Book"}
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
