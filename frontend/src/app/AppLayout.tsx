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
            {user?.role === "ADMIN" && (
              <>
                {/* TODO: add admin navigation links when admin pages are implemented */}
                <Link to="/admin/requests">Anfragen</Link>
              </>
            )}
            {user?.role === "CLUB_REPRESENTATIVE" && (
              <>
                {/* TODO: add club rep navigation when request pages are implemented */}
                <Link to="/my-requests">Meine Anfragen</Link>
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
