import { CheckCheck } from "lucide-react";
import { useEffect, useState } from "react";
import EmptyState from "../../components/EmptyState.jsx";
import { notificationsApi } from "../../services/api.js";
import { apiError, formatDate } from "../../utils/format.js";

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState([]);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      const page = await notificationsApi.list({ size: 20 });
      setNotifications(page.content);
    } catch (err) {
      setError(apiError(err));
    }
  };

  useEffect(() => {
    load();
  }, []);

  const markRead = async (id) => {
    await notificationsApi.markRead(id);
    await load();
  };

  return (
    <section className="page-stack">
      <div className="page-heading"><div><p className="eyebrow">Inbox</p><h1>Notifications</h1></div></div>
      {error ? <div className="alert alert-warning">{error}</div> : null}
      {notifications.length === 0 ? <EmptyState title="No notifications" /> : (
        <div className="list-stack">
          {notifications.map((item) => (
            <article className={`notification-row ${item.readAt ? "read" : ""}`} key={item.id}>
              <div>
                <h2>{item.title}</h2>
                <p>{item.message}</p>
                <span>{formatDate(item.sentAt)}</span>
              </div>
              {!item.readAt ? <button className="icon-button" type="button" onClick={() => markRead(item.id)} aria-label="Mark as read"><CheckCheck size={18} /></button> : null}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
