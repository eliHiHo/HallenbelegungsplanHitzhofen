import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { authApi } from "../../shared/api/auth";
import { ApiError } from "../../shared/api/client";
import type { CurrentUser } from "../../shared/types/api";

interface AuthState {
  user: CurrentUser | null;
  // true while the initial /auth/me request is in flight
  loading: boolean;
  login: (user: CurrentUser) => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  // Bootstrap: check whether the browser already has a valid session cookie
  useEffect(() => {
    authApi
      .me()
      .then(setUser)
      .catch((err) => {
        // 401 = no session, ignore silently
        if (err instanceof ApiError && err.status === 401) return;
        console.error("Failed to load current user", err);
      })
      .finally(() => setLoading(false));
  }, []);

  async function logout() {
    try {
      await authApi.logout();
    } catch {
      // best effort – clear local state regardless
    }
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login: setUser, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
