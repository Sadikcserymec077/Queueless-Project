const colors = {
  WAITING: "pill-warning",
  CALLED: "pill-info",
  COMPLETED: "pill-success",
  CANCELLED: "pill-muted",
  SKIPPED: "pill-danger",
  ACTIVE: "pill-success",
  INACTIVE: "pill-muted",
  PENDING: "pill-warning",
  APPROVED: "pill-success",
  REJECTED: "pill-danger",
  SUPER_ADMIN: "pill-danger",
  ORG_ADMIN: "pill-info",
  SUB_ADMIN: "pill-info",
  DOCTOR: "pill-success",
  STAFF: "pill-success",
  USER: "pill-muted"
};

export default function StatusPill({ value }) {
  return <span className={`status-pill ${colors[value] || "pill-muted"}`}>{value}</span>;
}
