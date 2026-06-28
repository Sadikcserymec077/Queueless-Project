import { Pencil, Plus, PowerOff, CalendarDays, X } from "lucide-react";
import { useEffect, useState } from "react";
import StatusPill from "../../components/StatusPill.jsx";
import { countersApi, organizationsApi } from "../../services/api.js";
import { apiError } from "../../utils/format.js";
import { useAuth } from "../../context/AuthContext.jsx";

const emptyForm = {
  counterName: "",
  counterNumber: 1,
  serviceType: "",
  bookingFee: 0,
  status: "ACTIVE"
};

export default function CounterManagement() {
  const { user } = useAuth();
  const [organizations, setOrganizations] = useState([]);
  const [organizationId, setOrganizationId] = useState("");
  const [counters, setCounters] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  
  // Scheduling state
  const [scheduleModalCounter, setScheduleModalCounter] = useState(null);
  const [scheduleDates, setScheduleDates] = useState([]);
  const [dailyCapacity, setDailyCapacity] = useState(100);
  const [newDateStr, setNewDateStr] = useState("");

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

  const loadCounters = async () => {
    if (!organizationId) return;
    setCounters(await countersApi.byOrganization(organizationId));
  };

  useEffect(() => {
    loadCounters().catch((err) => setError(apiError(err)));
  }, [organizationId]);

  const submit = async (event) => {
    event.preventDefault();
    const payload = { ...form, organizationId: Number(organizationId), counterNumber: Number(form.counterNumber), bookingFee: Number(form.bookingFee) };
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
      bookingFee: counter.bookingFee || 0,
      status: counter.status
    });
  };

  const disable = async (id) => {
    await countersApi.disable(id);
    await loadCounters();
  };

  const openSchedule = async (counter) => {
    setScheduleModalCounter(counter);
    setScheduleDates([]);
    setDailyCapacity(100);
    setNewDateStr("");
    try {
      const dates = await countersApi.availableDates(counter.id);
      setScheduleDates(dates);
    } catch (err) {
      console.warn("Failed to fetch current schedule", err);
    }
  };

  const addScheduleDate = () => {
    if (newDateStr && !scheduleDates.includes(newDateStr)) {
      setScheduleDates([...scheduleDates, newDateStr]);
      setNewDateStr("");
    }
  };

  const removeScheduleDate = (dateToRemove) => {
    setScheduleDates(scheduleDates.filter(d => d !== dateToRemove));
  };

  const saveSchedule = async () => {
    try {
      await countersApi.setSchedule(scheduleModalCounter.id, {
        dates: scheduleDates,
        dailyCapacity: Number(dailyCapacity)
      });
      setScheduleModalCounter(null);
      alert("Schedule updated successfully");
    } catch (err) {
      setError(apiError(err));
    }
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
        <input type="number" min={0} step="0.01" placeholder="Fee (₹)" value={form.bookingFee} onChange={(event) => setForm({ ...form, bookingFee: event.target.value })} />
        <select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select>
        <button className="primary-action" type="submit"><Plus size={18} />{editingId ? "Update" : "Create"}</button>
      </form>

      <div className="table-responsive">
        <table>
          <thead><tr><th>Counter</th><th>Number</th><th>Service</th><th>Fee</th><th>Waiting</th><th>Current</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {counters.map((counter) => (
              <tr key={counter.id}>
                <td>{counter.counterName}</td>
                <td>{counter.counterNumber}</td>
                <td>{counter.serviceType}</td>
                <td>₹{counter.bookingFee || 0}</td>
                <td>{counter.waitingTokens}</td>
                <td>{counter.currentToken || "None"}</td>
                <td><StatusPill value={counter.status} /></td>
                <td className="row-actions">
                  <button className="icon-button" type="button" onClick={() => edit(counter)} aria-label="Edit counter"><Pencil size={17} /></button>
                  <button className="icon-button" type="button" onClick={() => openSchedule(counter)} aria-label="Manage schedule"><CalendarDays size={17} /></button>
                  <button className="icon-button danger" type="button" onClick={() => disable(counter.id)} aria-label="Disable counter"><PowerOff size={17} /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {scheduleModalCounter && (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', backgroundColor: 'rgba(0,0,0,0.6)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: 'var(--bg-card, #fff)', padding: '2rem', borderRadius: '12px', maxWidth: '450px', width: '90%', boxShadow: '0 4px 20px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ margin: 0 }}>Schedule Dates</h2>
              <button onClick={() => setScheduleModalCounter(null)} style={{ background: 'none', border: 'none', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Daily Capacity</label>
              <input type="number" min="1" className="form-control" value={dailyCapacity} onChange={e => setDailyCapacity(e.target.value)} />
            </div>

            <div style={{ marginBottom: '1.5rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>Add Date (YYYY-MM-DD)</label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input type="date" className="form-control" style={{ flex: 1 }} value={newDateStr} onChange={e => setNewDateStr(e.target.value)} min={new Date().toISOString().split('T')[0]} />
                <button type="button" className="secondary-action" onClick={addScheduleDate}>Add</button>
              </div>
            </div>

            <div style={{ marginBottom: '1.5rem', maxHeight: '150px', overflowY: 'auto', border: '1px solid var(--border)', borderRadius: '4px', padding: '0.5rem' }}>
              {scheduleDates.length === 0 ? <div style={{ color: 'var(--text-muted)' }}>No future dates configured.</div> : null}
              {scheduleDates.map(date => (
                <div key={date} style={{ display: 'flex', justifyContent: 'space-between', padding: '0.5rem', borderBottom: '1px solid var(--border)' }}>
                  <span>{date}</span>
                  <button type="button" onClick={() => removeScheduleDate(date)} style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer' }}><X size={14} /></button>
                </div>
              ))}
            </div>

            <button className="primary-action" style={{ width: '100%', justifyContent: 'center' }} onClick={saveSchedule}>Save Schedule</button>
          </div>
        </div>
      )}
    </section>
  );
}
