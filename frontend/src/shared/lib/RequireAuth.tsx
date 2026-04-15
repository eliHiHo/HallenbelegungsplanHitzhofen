import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthContext";
import type { Role } from "../types/api";

interface Props {
  children: React.ReactNode;
  // If provided, also checks that the user has this role.
  // Authenticated users without the required role see a plain "Access denied" message.
  role?: Role;
}

/**
 * Redirects unauthenticated users to /login, preserving the intended path.
 * If `role` is specified, renders a 403 message for authenticated users who lack it.
 * Renders nothing while the auth bootstrap is still in flight.
 */
export default function RequireAuth({ children, role }: Props) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (role && user.role !== role) {
    return <p style={{ padding: "2rem" }}>Kein Zugriff.</p>;
  }

  return <>{children}</>;
}
