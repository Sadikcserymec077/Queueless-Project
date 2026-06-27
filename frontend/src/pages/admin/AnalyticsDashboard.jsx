import { Bar, Doughnut, Line } from "react-chartjs-2";
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip
} from "chart.js";
import { useEffect, useState } from "react";
import { analyticsApi } from "../../services/api.js";
import { apiError } from "../../utils/format.js";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend);

const options = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false }
  },
  scales: {
    x: { grid: { display: false } },
    y: { beginAtZero: true, grid: { color: "#edf2f7" } }
  }
};

function chartData(points = [], label, color) {
  return {
    labels: points.map((point) => point.label),
    datasets: [{
      label,
      data: points.map((point) => point.value),
      backgroundColor: color,
      borderColor: color,
      tension: 0.35,
      fill: false
    }]
  };
}

export default function AnalyticsDashboard() {
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

  const serviceData = {
    labels: (stats?.serviceEfficiency || []).map((point) => point.label),
    datasets: [{
      data: (stats?.serviceEfficiency || []).map((point) => point.value),
      backgroundColor: ["#1f9d6a", "#d95f45", "#64748b"]
    }]
  };

  return (
    <section className="page-stack">
      <div className="page-heading"><div><p className="eyebrow">Insights</p><h1>Analytics</h1></div></div>
      {error ? <div className="alert alert-warning">{error}</div> : null}
      <div className="chart-grid">
        <article className="chart-panel">
          <h2>Daily visitors</h2>
          <Line data={chartData(stats?.dailyVisitors, "Visitors", "#16697a")} options={options} />
        </article>
        <article className="chart-panel">
          <h2>Weekly visitors</h2>
          <Bar data={chartData(stats?.weeklyVisitors, "Visitors", "#d99036")} options={options} />
        </article>
        <article className="chart-panel">
          <h2>Monthly visitors</h2>
          <Bar data={chartData(stats?.monthlyVisitors, "Visitors", "#4f46e5")} options={options} />
        </article>
        <article className="chart-panel">
          <h2>Peak hours</h2>
          <Line data={chartData(stats?.peakHours, "Tokens", "#0f766e")} options={options} />
        </article>
        <article className="chart-panel">
          <h2>Average waiting time</h2>
          <Line data={chartData(stats?.averageWaitingTime, "Minutes", "#b45309")} options={options} />
        </article>
        <article className="chart-panel">
          <h2>Service efficiency</h2>
          <Doughnut data={serviceData} options={{ responsive: true, maintainAspectRatio: false }} />
        </article>
      </div>
    </section>
  );
}
