import { Pencil, Plus, PowerOff } from "lucide-react";
import { useEffect, useState } from "react";
import StatusPill from "../../components/StatusPill.jsx";
import { countersApi, organizationsApi } from "../../services/api.js";
import { apiError } from "../../utils/format.js";

const emptyForm = {
  counterName: "",
  counterNumber: 1,
  serviceType: "",
  status: "ACTIVE"
};

export default function CounterManagement() {
  const [organizations, setOrganizations] = useState([]);
  const [organizationId, setOrganizationId] = useState("");
  const [counters, setCounters] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadOrganizations() {
      try {
        const page = await organizationsApi.search({ size: 50 });
        setOrganizations(page.content);
        if (page.content.length) setOrganizationId(String(page.content[0].id));
      } catch (err) {
        setError(apiError(err));
      }
    }
    loadOrganizations();
  }, []);

  const loadCounters = async () => {
    if (!organizationId) return;
    setCounters(await countersApi.byOrganization(organizationId));
  };

  useEffect(() => {
    loadCounters().catch((err) => setError(apiError(err)));
  }, [organizationId]);

  const submit = async (event) => {
    event.preventDefault();
    const payload = { ...form, organizationId: Number(organizationId), counterNumber: Number(form.counterNumber) };
    try {
      if (editingId) {
        await countersApi.update(editingId, payload);
      } else {
        await countersApi.create(payload);
      }
      setForm(emptyForm);
      setEditingId(null);
      await loadCounters();
    } catch (err) {
      setError(apiError(err));
    }
  };

  const edit = (counter) => {
    setEditingId(counter.id);
    setOrganizationId(String(counter.organizationId));
    setForm({
      counterName: counter.counterName,
      counterNumber: counter.counterNumber,
      serviceType: counter.serviceType,
      status: counter.status
    });
  };

  const disable = async (id) => {
    await countersApi.disable(id);
    await loadCounters();
  };

  return (
    <section className="page-stack">
      <div className="page-heading"><div><p className="eyebrow">Admin</p><h1>Counters</h1></div></div>
      {error ? <div className="alert alert-warning">{error}</div> : null}

      <div className="toolbar">
        <select value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} aria-label="Organization">
          {organizations.map((organization) => <option key={organization.id} value={organization.id}>{organization.name}</option>)}
        </select>
      </div>

      <form className="management-form" onSubmit={submit}>
        <input required placeholder="Counter name" value={form.counterName} onChange={(event) => setForm({ ...form, counterName: event.target.value })} />
        <input required min={1} type="number" placeholder="Number" value={form.counterNumber} onChange={(event) => setForm({ ...form, counterNumber: event.target.value })} />
        <input required placeholder="Service type" value={form.serviceType} onChange={(event) => setForm({ ...form, serviceType: event.target.value })} />
        <select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select>
        <button className="primary-action" type="submit"><Plus size={18} />{editingId ? "Update" : "Create"}</button>
      </form>

      <div className="table-responsive">
        <table>
          <thead><tr><th>Counter</th><th>Number</th><th>Service</th><th>Waiting</th><th>Current</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {counters.map((counter) => (
              <tr key={counter.id}>
                <td>{counter.counterName}</td>
                <td>{counter.counterNumber}</td>
                <td>{counter.serviceType}</td>
                <td>{counter.waitingTokens}</td>
                <td>{counter.currentToken || "None"}</td>
                <td><StatusPill value={counter.status} /></td>
                <td className="row-actions">
                  <button className="icon-button" type="button" onClick={() => edit(counter)} aria-label="Edit counter"><Pencil size={17} /></button>
                  <button className="icon-button danger" type="button" onClick={() => disable(counter.id)} aria-label="Disable counter"><PowerOff size={17} /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
