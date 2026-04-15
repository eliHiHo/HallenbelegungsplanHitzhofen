import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../../shared/api/auth";
import { ApiError } from "../../shared/api/client";
import { useAuth } from "./AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const res = await authApi.login({ email, password });
      // After login the session cookie is set by the backend.
      // We derive the CurrentUser shape from the LoginResponse.
      login({
        id: res.userId,
        firstName: res.firstName,
        lastName: res.lastName,
        fullName: `${res.firstName} ${res.lastName}`,
        email: res.email,
        role: res.role,
        active: true,
      });
      navigate("/");
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        setError("Too many failed attempts. Please wait and try again.");
      } else if (err instanceof ApiError && err.status === 401) {
        setError("Invalid email or password.");
      } else {
        setError("Login failed. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <h1>Hallenbelegungsplan</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">E-Mail</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
          />
        </div>
        <div>
          <label htmlFor="password">Passwort</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </div>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? "Anmelden…" : "Anmelden"}
        </button>
        <a href="/forgot-password">Passwort vergessen?</a>
      </form>
    </div>
  );
}
