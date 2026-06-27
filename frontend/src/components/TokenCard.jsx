import { CalendarClock, Hourglass, UsersRound } from "lucide-react";
import StatusPill from "./StatusPill.jsx";
import { formatDate, minutesLabel } from "../utils/format.js";

export default function TokenCard({ token, compact = false }) {
  if (!token) return null;

  return (
    <article className="token-card">
      <div className="token-card-head">
        <div>
          <p className="eyebrow">{token.organizationName}</p>
          <h2>{token.tokenNumber}</h2>
        </div>
        <StatusPill value={token.status} />
      </div>
      <dl className="token-meta">
        <div>
          <dt><CalendarClock size={16} /> Counter</dt>
          <dd>{token.counterName}</dd>
        </div>
        <div>
          <dt><UsersRound size={16} /> Position</dt>
          <dd>{token.queuePosition === 0 ? "Serving" : token.queuePosition ?? "Closed"}</dd>
        </div>
        <div>
          <dt><Hourglass size={16} /> Estimate</dt>
          <dd>{minutesLabel(token.estimatedWaitTimeMinutes)}</dd>
        </div>
      </dl>
      {!compact && (
        <div className="token-footer">
          <span>Booked {formatDate(token.bookingTime)}</span>
          {token.qrCodeData ? <img src={token.qrCodeData} alt={`QR for ${token.tokenNumber}`} /> : null}
        </div>
      )}
    </article>
  );
}
