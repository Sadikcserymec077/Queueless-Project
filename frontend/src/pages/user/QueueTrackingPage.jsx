import { RefreshCw, XCircle } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import EmptyState from "../../components/EmptyState.jsx";
import TokenCard from "../../components/TokenCard.jsx";
import { subscribeToCounter } from "../../services/socket.js";
import { tokensApi } from "../../services/api.js";
import { apiError, formatDate, minutesLabel } from "../../utils/format.js";

export default function QueueTrackingPage() {
  const [active, setActive] = useState(null);
  const [status, setStatus] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      setError("");
      const token = await tokensApi.active();
      setActive(token);
      setStatus(token ? await tokensApi.status(token.id) : null);
    } catch (err) {
      setError(apiError(err));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!active?.counterId) return undefined;
    return subscribeToCounter(active.counterId, load);
  }, [active?.counterId, load]);

  const requestCancel = async () => {
    if (!active) return;
    if (!confirm("Are you sure you want to request cancellation for this token?")) return;
    try {
      await tokensApi.requestCancel(active.id);
      alert("Cancellation request sent to the organization.");
    } catch (err) {
      setError(apiError(err));
    }
  };

  const requestDelay = async () => {
    if (!active) return;
    if (!confirm("Are you sure you want to request to come late? Your token will be moved to the end of the line if approved.")) return;
    try {
      await tokensApi.requestDelay(active.id);
      alert("Come late request sent to the organization.");
    } catch (err) {
      setError(apiError(err));
    }
  };

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Live queue</p>
          <h1>Queue tracking</h1>
        </div>
        <button className="secondary-action" type="button" onClick={load}><RefreshCw size={18} /> Refresh</button>
      </div>

      {error ? <div className="alert alert-warning">{error}</div> : null}
      {!active ? (
        <EmptyState title="No live queue" text="Book a token to watch queue movement in real time." />
      ) : (
        <>
          {/* Live Banner */}
          <div style={{
            background: "linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)",
            color: "white",
            padding: "1.5rem",
            borderRadius: "16px",
            marginBottom: "1rem",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            boxShadow: "0 10px 15px -3px rgba(79, 70, 229, 0.3)"
          }}>
            <p style={{ margin: 0, fontSize: "0.875rem", textTransform: "uppercase", letterSpacing: "0.05em", opacity: 0.9 }}>Now Serving</p>
            <h2 style={{ margin: "0.5rem 0", fontSize: "3rem", fontWeight: "800", textShadow: "0 2px 4px rgba(0,0,0,0.2)" }}>
              {status?.currentTokenBeingServed || "Waiting..."}
            </h2>
          </div>

          <TokenCard token={active} />
          
          <div className="queue-panel">
            <div><span>People ahead of you</span><strong>{status?.peopleAhead ?? 0}</strong></div>
            <div><span>Estimated wait</span><strong>{minutesLabel(status?.estimatedWaitingTimeMinutes)}</strong></div>
            <div><span>Expected turn</span><strong>{formatDate(status?.expectedTurnTime)}</strong></div>
          </div>
          <div className="tracking-actions" style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
            <button className="secondary-action" type="button" onClick={requestDelay}>🏃‍♂️ Come Late</button>
            <button className="danger-action" type="button" onClick={requestCancel}><XCircle size={18} /> Request Cancel</button>
          </div>
        </>
      )}
    </section>
  );
}
