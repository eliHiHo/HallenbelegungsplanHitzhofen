import { Outlet, Link, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";

export default function AppLayout() {
  const { user, loading, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  if (loading) {
    return <div className="app-loading">Laden…</div>;
  }

  return (
    <div className="app">
      <header className="app-header">
        <nav className="app-nav">
          <Link to="/" className="app-nav-brand">
            Hallenbelegungsplan
          </Link>
          <div className="app-nav-links">
            <Link to="/">Kalender</Link>
            {user?.role === "CLUB_REPRESENTATIVE" && (
              <>
                <Link to="/my-requests">Anfragen</Link>
                <Link to="/my-statistics">Statistiken</Link>
              </>
            )}
            {user?.role === "ADMIN" && (
              <>
                <Link to="/admin/requests">Anfragen</Link>
                <Link to="/admin/series-requests">Serienanfragen</Link>
                <Link to="/admin/blocked-times">Sperrzeiten</Link>
                <Link to="/admin/statistics">Statistiken</Link>
                <Link to="/admin/users">Benutzer</Link>
              </>
            )}
          </div>
          <div className="app-nav-user">
            {user ? (
              <>
                <span>{user.fullName}</span>
                <button onClick={handleLogout}>Abmelden</button>
              </>
            ) : (
              <Link to="/login">Anmelden</Link>
            )}
          </div>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
