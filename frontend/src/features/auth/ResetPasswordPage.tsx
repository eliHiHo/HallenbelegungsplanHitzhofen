import { useState, type FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { authApi } from "../../shared/api/auth";
import { ApiError } from "../../shared/api/client";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await authApi.resetPassword({ token, newPassword });
      navigate("/login");
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("Der Link ist ungültig oder abgelaufen.");
      } else {
        setError("Fehler beim Zurücksetzen. Bitte versuche es erneut.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (!token) {
    return (
      <div className="reset-password-page">
        <p>Ungültiger Link.</p>
        <a href="/login">Zur Anmeldung</a>
      </div>
    );
  }

  return (
    <div className="reset-password-page">
      <h1>Neues Passwort setzen</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="password">Neues Passwort</label>
          <input
            id="password"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            autoComplete="new-password"
          />
        </div>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? "Speichern…" : "Passwort speichern"}
        </button>
      </form>
    </div>
  );
}
