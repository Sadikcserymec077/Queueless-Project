import { Plus, Trash2, UserPlus } from "lucide-react";
import { useEffect, useState } from "react";
import StatusPill from "../../components/StatusPill.jsx";
import { organizationUsersApi } from "../../services/api.js";
import { apiError } from "../../utils/format.js";

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState({ email: "", role: "SUB_ADMIN" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const load = async () => {
    try {
      const data = await organizationUsersApi.list();
      setUsers(data);
    } catch (err) {
      setError(apiError(err));
    }
  };

  useEffect(() => {
    load();
  }, []);

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await organizationUsersApi.add(form);
      setForm({ email: "", role: "SUB_ADMIN" });
      await load();
    } catch (err) {
      setError(apiError(err));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (email) => {
    if (!window.confirm(`Are you sure you want to remove ${email} from the organization?`)) return;
    try {
      await organizationUsersApi.remove(email);
      await load();
    } catch (err) {
      setError(apiError(err));
    }
  };

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>User Management</h1>
        </div>
      </div>
      
      {error ? <div className="alert alert-warning">{error}</div> : null}

      <form className="management-form" onSubmit={submit}>
        <input 
          type="email" 
          required 
          placeholder="User Email Address" 
          value={form.email} 
          onChange={(event) => setForm({ ...form, email: event.target.value })} 
        />
        <button className="primary-action" type="submit" disabled={busy}>
          <UserPlus size={18} />
          Add User
        </button>
      </form>

      <div className="table-responsive">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.length === 0 ? (
              <tr>
                <td colSpan="5" style={{ textAlign: "center", padding: "2rem" }}>
                  No users found in your organization.
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.email}</td>
                  <td><StatusPill value={user.role} /></td>
                  <td><StatusPill value={user.enabled ? "ACTIVE" : "DISABLED"} /></td>
                  <td className="row-actions">
                    <button 
                      className="icon-button danger" 
                      type="button" 
                      onClick={() => remove(user.email)} 
                      aria-label="Remove user from organization"
                    >
                      <Trash2 size={17} />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
