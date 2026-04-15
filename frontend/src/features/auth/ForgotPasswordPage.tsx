import { useState, type FormEvent } from "react";
import { authApi } from "../../shared/api/auth";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await authApi.forgotPassword({ email });
    } finally {
      // Always show success message to avoid user enumeration
      setSent(true);
      setSubmitting(false);
    }
  }

  if (sent) {
    return (
      <div className="forgot-password-page">
        <p>
          Falls ein Konto mit dieser E-Mail-Adresse existiert, wurde eine
          E-Mail mit einem Link zum Zurücksetzen des Passworts gesendet.
        </p>
        <a href="/login">Zurück zur Anmeldung</a>
      </div>
    );
  }

  return (
    <div className="forgot-password-page">
      <h1>Passwort zurücksetzen</h1>
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
        <button type="submit" disabled={submitting}>
          {submitting ? "Senden…" : "Link senden"}
        </button>
      </form>
      <a href="/login">Zurück zur Anmeldung</a>
    </div>
  );
}
