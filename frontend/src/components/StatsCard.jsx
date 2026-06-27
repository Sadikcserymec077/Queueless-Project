export default function StatsCard({ icon: Icon, label, value, tone = "primary" }) {
  return (
    <div className={`stats-card stats-${tone}`}>
      <div className="stats-icon">{Icon ? <Icon size={20} /> : null}</div>
      <div>
        <p>{label}</p>
        <strong>{value}</strong>
      </div>
    </div>
  );
}
