import { Pencil, Plus, Search, Trash2, CheckCircle, XCircle, Eye, EyeOff } from "lucide-react";
import { useEffect, useState } from "react";
import StatusPill from "../../components/StatusPill.jsx";
import { organizationsApi } from "../../services/api.js";
import { apiError } from "../../utils/format.js";

const emptyForm = {
  name: "",
  type: "HOSPITAL",
  address: "",
  contactNumber: "",
  email: "",
  workingHours: "",
  adminName: "",
  adminEmail: "",
  adminPassword: ""
};

const types = ["HOSPITAL", "BANK", "COLLEGE", "GOVERNMENT_OFFICE", "SERVICE_CENTER"];

export default function OrganizationManagement() {
  const [items, setItems] = useState([]);
  const [q, setQ] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const load = async () => {
    try {
      const page = await organizationsApi.search({ q: q || undefined, includeInactive: true, size: 50 });
      setItems(page.content);
    } catch (err) {
      setError(apiError(err));
    }
  };

  useEffect(() => {
    const timer = setTimeout(load, 200);
    return () => clearTimeout(timer);
  }, [q]);

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    try {
      if (editingId) {
        await organizationsApi.update(editingId, form);
      } else {
        await organizationsApi.create(form);
      }
      setForm(emptyForm);
      setEditingId(null);
      await load();
    } catch (err) {
      setError(apiError(err));
    }
  };

  const edit = (organization) => {
    setEditingId(organization.id);
    setForm({
      name: organization.name,
      type: organization.type,
      address: organization.address,
      contactNumber: organization.contactNumber,
      email: organization.email,
      workingHours: organization.workingHours
    });
  };

  const remove = async (id) => {
    await organizationsApi.remove(id);
    await load();
  };

  const approve = async (id) => {
    await organizationsApi.approve(id);
    await load();
  };

  const reject = async (id) => {
    await organizationsApi.reject(id);
    await load();
  };

  return (
    <section className="page-stack">
      <div className="page-heading"><div><p className="eyebrow">Admin</p><h1>Organizations</h1></div></div>
      {error ? <div className="alert alert-warning">{error}</div> : null}

      <form className="management-form" onSubmit={submit}>
        <input required placeholder="Organization name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
        <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>{types.map((type) => <option key={type} value={type}>{type.replaceAll("_", " ")}</option>)}</select>
        <input required placeholder="Address" value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} />
        <input required placeholder="Contact number" value={form.contactNumber} onChange={(event) => setForm({ ...form, contactNumber: event.target.value })} />
        <input required type="email" placeholder="Organization Email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
        <input required placeholder="Working hours" value={form.workingHours} onChange={(event) => setForm({ ...form, workingHours: event.target.value })} />
        {!editingId && (
          <fieldset style={{ display: "contents" }}>
            <p className="eyebrow" style={{ gridColumn: "1 / -1", margin: "1rem 0 0.5rem" }}>Admin Credentials</p>
            <input required placeholder="Admin Name" value={form.adminName} onChange={(event) => setForm({ ...form, adminName: event.target.value })} />
            <input required type="email" placeholder="Admin Email (Login)" value={form.adminEmail} onChange={(event) => setForm({ ...form, adminEmail: event.target.value })} />
            <div style={{ position: "relative", width: "100%" }}>
              <input required type={showPassword ? "text" : "password"} placeholder="Admin Password" value={form.adminPassword} onChange={(event) => setForm({ ...form, adminPassword: event.target.value })} style={{ width: "100%", paddingRight: "40px" }} />
              <button type="button" onClick={() => setShowPassword(!showPassword)} style={{ position: "absolute", right: "10px", top: "50%", transform: "translateY(-50%)", background: "transparent", border: "none", cursor: "pointer", color: "inherit", opacity: 0.7, padding: 0, display: "flex" }}>
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </fieldset>
        )}
        <button className="primary-action" type="submit" style={{ gridColumn: "1 / -1" }}><Plus size={18} />{editingId ? "Update Organization" : "Create Organization & Admin"}</button>
      </form>

      <div className="toolbar">
        <label className="search-box"><Search size={18} /><input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search organizations" /></label>
      </div>
      <div className="table-responsive">
        <table>
          <thead><tr><th>Name</th><th>Type</th><th>Counters</th><th>Queue</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.name}</td>
                <td>{item.type.replaceAll("_", " ")}</td>
                <td>{item.activeCounters}</td>
                <td>{item.activeTokens}</td>
                <td><StatusPill value={item.status || (item.active ? "ACTIVE" : "INACTIVE")} /></td>
                <td className="row-actions">
                  {item.status === 'PENDING' && (
                    <>
                      <button className="icon-button success" type="button" onClick={() => approve(item.id)} aria-label="Approve organization" style={{ color: 'var(--color-success)' }}><CheckCircle size={17} /></button>
                      <button className="icon-button danger" type="button" onClick={() => reject(item.id)} aria-label="Reject organization"><XCircle size={17} /></button>
                    </>
                  )}
                  <button className="icon-button" type="button" onClick={() => edit(item)} aria-label="Edit organization"><Pencil size={17} /></button>
                  <button className="icon-button danger" type="button" onClick={() => remove(item.id)} aria-label="Disable organization"><Trash2 size={17} /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
