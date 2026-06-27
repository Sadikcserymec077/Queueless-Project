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

  const cancel = async () => {
    if (!active) return;
    try {
      await tokensApi.cancel(active.id);
      await load();
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
          <TokenCard token={active} />
          <div className="queue-panel">
            <div><span>Current token</span><strong>{status?.currentTokenBeingServed || "Waiting to start"}</strong></div>
            <div><span>People ahead</span><strong>{status?.peopleAhead ?? 0}</strong></div>
            <div><span>Estimated wait</span><strong>{minutesLabel(status?.estimatedWaitingTimeMinutes)}</strong></div>
            <div><span>Expected turn</span><strong>{formatDate(status?.expectedTurnTime)}</strong></div>
          </div>
          <button className="danger-action" type="button" onClick={cancel}><XCircle size={18} /> Cancel token</button>
        </>
      )}
    </section>
  );
}
