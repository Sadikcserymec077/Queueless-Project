import { Bell, Building2, CalendarPlus, ChartNoAxesCombined, LayoutDashboard, LogOut, Menu, MonitorDot, UserRound, Home } from "lucide-react";
import { useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

export default function TopNav() {
  const [open, setOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const links = (user?.role === "SUPER_ADMIN" || user?.role === "ORG_ADMIN" || user?.role === "SUB_ADMIN")
    ? [
        ["Dashboard", "/admin/dashboard", LayoutDashboard],
        ...(user?.role === "SUPER_ADMIN" ? [["Organizations", "/admin/organizations", Building2]] : []),
        ["Users", "/admin/users", UserRound],
        ["Counters", "/admin/counters", MonitorDot],
        ["Queue", "/admin/queue", CalendarPlus],
        ["Analytics", "/admin/analytics", ChartNoAxesCombined]
      ]
    : user
      ? [
          ["Home", "/", Home],
          ["Dashboard", "/user/dashboard", LayoutDashboard],
          ["Book", "/user/book", CalendarPlus],
          ["Track", "/user/track", MonitorDot],
          ["Notifications", "/user/notifications", Bell],
          ["Profile", "/user/profile", UserRound]
        ]
      : [
          ["Home", "/", Home]
        ];

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <header className="top-nav">
      <Link to="/" className="brand">
        <span>QL</span>
        QueueLess AI
      </Link>
      <button className="icon-button nav-toggle" type="button" onClick={() => setOpen((value) => !value)} aria-label="Toggle navigation">
        <Menu size={20} />
      </button>
      <nav className={open ? "open" : ""}>
        {links.map(([label, path, Icon]) => (
          <NavLink key={path} to={path} onClick={() => setOpen(false)} className={({ isActive }) => isActive ? "active" : ""}>
            <Icon size={17} />
            {label}
          </NavLink>
        ))}
        {!user ? (
          <>
            <NavLink to="/login" onClick={() => setOpen(false)}>Login</NavLink>
            <NavLink to="/register" onClick={() => setOpen(false)}>Register</NavLink>
          </>
        ) : (
          <button className="text-button" type="button" onClick={handleLogout}>
            <LogOut size={17} />
            Logout
          </button>
        )}
      </nav>
    </header>
  );
}
