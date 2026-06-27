import { CheckCircle2, Play, SkipForward, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";
import StatusPill from "../../components/StatusPill.jsx";
import TokenCard from "../../components/TokenCard.jsx";
import { countersApi, organizationsApi, tokensApi } from "../../services/api.js";
import { subscribeToCounter } from "../../services/socket.js";
import { apiError, formatDate } from "../../utils/format.js";
import { useAuth } from "../../context/AuthContext.jsx";

export default function QueueMonitoring() {
  const { user } = useAuth();
  const [organizations, setOrganizations] = useState([]);
  const [organizationId, setOrganizationId] = useState("");
  const [counters, setCounters] = useState([]);
  const [counterId, setCounterId] = useState("");
  const [queue, setQueue] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadOrganizations() {
      try {
        const page = await organizationsApi.search({ size: 50 });
        let availableOrgs = page.content;
        
        if (user.role !== "SUPER_ADMIN" && user.organizationId) {
          availableOrgs = availableOrgs.filter(org => org.id === user.organizationId);
        }
        
        setOrganizations(availableOrgs);
        if (availableOrgs.length) setOrganizationId(String(availableOrgs[0].id));
      } catch (err) {
        setError(apiError(err));
      }
    }
    loadOrganizations();
  }, []);

  useEffect(() => {
    if (!organizationId) return;
    async function loadCounters() {
      const list = await countersApi.byOrganization(organizationId);
      setCounters(list);
      setCounterId(list[0]?.id ? String(list[0].id) : "");
    }
    loadCounters().catch((err) => setError(apiError(err)));
  }, [organizationId]);

  const loadQueue = async () => {
    if (!counterId) return;
    try {
      setQueue(await tokensApi.counterQueue(counterId));
    } catch (err) {
      setError(apiError(err));
    }
  };

  useEffect(() => {
    loadQueue();
  }, [counterId]);

  useEffect(() => {
    if (!counterId) return undefined;
    return subscribeToCounter(counterId, setQueue);
  }, [counterId]);

  const action = async (fn) => {
    try {
      await fn();
      await loadQueue();
    } catch (err) {
      setError(apiError(err));
    }
  };

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div><p className="eyebrow">Live operations</p><h1>Queue monitoring</h1></div>
        <button className="secondary-action" type="button" onClick={loadQueue}><RefreshCw size={18} /> Refresh</button>
      </div>
      {error ? <div className="alert alert-warning">{error}</div> : null}

      <div className="toolbar">
        <select value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} aria-label="Organization">
          {organizations.map((organization) => <option key={organization.id} value={organization.id}>{organization.name}</option>)}
        </select>
        <select value={counterId} onChange={(event) => setCounterId(event.target.value)} aria-label="Counter">
          {counters.map((counter) => <option key={counter.id} value={counter.id}>{counter.counterName}</option>)}
        </select>
      </div>

      <div className="queue-actions">
        <button className="primary-action" type="button" disabled={!counterId} onClick={() => action(() => tokensApi.callNext(counterId))}><Play size={18} /> Call next</button>
        <button className="secondary-action" type="button" disabled={!queue?.currentToken} onClick={() => action(() => tokensApi.complete(queue.currentToken.id))}><CheckCircle2 size={18} /> Complete</button>
        <button className="danger-action" type="button" disabled={!queue?.currentToken} onClick={() => action(() => tokensApi.skip(queue.currentToken.id))}><SkipForward size={18} /> Skip</button>
      </div>

      {queue?.currentToken ? <TokenCard token={queue.currentToken} compact /> : <div className="empty-state"><h2>No token being served</h2></div>}

      <section className="data-section">
        <h2>Waiting tokens</h2>
        <div className="table-responsive">
          <table>
            <thead><tr><th>Token</th><th>User</th><th>Position</th><th>Estimate</th><th>Status</th><th>Booked</th></tr></thead>
            <tbody>
              {(queue?.waitingTokens || []).map((token) => (
                <tr key={token.id}>
                  <td>{token.tokenNumber}</td>
                  <td>{token.userName}</td>
                  <td>{token.queuePosition}</td>
                  <td>{token.estimatedWaitTimeMinutes} min</td>
                  <td><StatusPill value={token.status} /></td>
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
