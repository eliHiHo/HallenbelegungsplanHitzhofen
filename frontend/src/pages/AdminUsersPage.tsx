import { useState, type FormEvent } from "react";
import { useUsers, useCreateUser, useUpdateUser } from "../features/users/useUsers";
import { ApiError } from "../shared/api/client";
import type { User, UserCreate, UserUpdate, Role } from "../shared/types/api";

const ROLE_LABELS: Record<Role, string> = {
  ADMIN: "Administrator",
  CLUB_REPRESENTATIVE: "Vereinsvertreter",
};

// ---- Create User Modal ----

function CreateUserModal({
  onClose,
  onSuccess,
}: {
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("CLUB_REPRESENTATIVE");
  const [error, setError] = useState<string | null>(null);

  const { mutateAsync: createUser, isPending } = useCreateUser();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const data: UserCreate = { firstName, lastName, email, password, role };

    try {
      await createUser(data);
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("Ungültige Eingabe. Bitte alle Felder prüfen.");
      } else if (err instanceof ApiError && err.status === 409) {
        setError("Diese E-Mail-Adresse ist bereits vergeben.");
      } else {
        setError("Fehler beim Anlegen. Bitte erneut versuchen.");
      }
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>Neuer Benutzer</h2>

        <form onSubmit={handleSubmit} className="request-form">
          <div className="form-field">
            <label htmlFor="cu-firstname">Vorname</label>
            <input
              id="cu-firstname"
              type="text"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="cu-lastname">Nachname</label>
            <input
              id="cu-lastname"
              type="text"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="cu-email">E-Mail</label>
            <input
              id="cu-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="cu-password">Passwort</label>
            <input
              id="cu-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
            />
          </div>

          <div className="form-field">
            <label htmlFor="cu-role">Rolle</label>
            <select
              id="cu-role"
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              required
            >
              <option value="CLUB_REPRESENTATIVE">Vereinsvertreter</option>
              <option value="ADMIN">Administrator</option>
            </select>
          </div>

          {error && <p className="error">{error}</p>}

          <div className="form-actions">
            <button type="button" onClick={onClose} disabled={isPending}>
              Abbrechen
            </button>
            <button type="submit" disabled={isPending}>
              {isPending ? "Wird angelegt…" : "Benutzer anlegen"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ---- Edit User Modal ----

function EditUserModal({
  user,
  onClose,
  onSuccess,
}: {
  user: User;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [firstName, setFirstName] = useState(user.firstName);
  const [lastName, setLastName] = useState(user.lastName);
  const [email, setEmail] = useState(user.email);
  const [role, setRole] = useState<Role>(user.role);
  const [active, setActive] = useState(user.active);
  const [error, setError] = useState<string | null>(null);

  const { mutateAsync: updateUser, isPending } = useUpdateUser();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const data: UserUpdate = { firstName, lastName, email, role, active };

    try {
      await updateUser({ id: user.id, data });
      onSuccess();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("Ungültige Eingabe. Bitte alle Felder prüfen.");
      } else if (err instanceof ApiError && err.status === 409) {
        setError("Diese E-Mail-Adresse ist bereits vergeben.");
      } else {
        setError("Fehler beim Speichern. Bitte erneut versuchen.");
      }
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>✕</button>
        <h2>Benutzer bearbeiten</h2>

        <form onSubmit={handleSubmit} className="request-form">
          <div className="form-field">
            <label htmlFor="eu-firstname">Vorname</label>
            <input
              id="eu-firstname"
              type="text"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="eu-lastname">Nachname</label>
            <input
              id="eu-lastname"
              type="text"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="eu-email">E-Mail</label>
            <input
              id="eu-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="eu-role">Rolle</label>
            <select
              id="eu-role"
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              required
            >
              <option value="CLUB_REPRESENTATIVE">Vereinsvertreter</option>
              <option value="ADMIN">Administrator</option>
            </select>
          </div>

          <div className="form-field">
            <label>
              <input
                type="checkbox"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                style={{ marginRight: "0.5rem" }}
              />
              Aktiv
            </label>
          </div>

          {error && <p className="error">{error}</p>}

          <div className="form-actions">
            <button type="button" onClick={onClose} disabled={isPending}>
              Abbrechen
            </button>
            <button type="submit" disabled={isPending}>
              {isPending ? "Wird gespeichert…" : "Speichern"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ---- Page ----

export default function AdminUsersPage() {
  const { data: users, isLoading, error } = useUsers();
  const [showCreate, setShowCreate] = useState(false);
  const [editUser, setEditUser] = useState<User | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  function handleSuccess(message: string) {
    setShowCreate(false);
    setEditUser(null);
    setSuccessMessage(message);
    setTimeout(() => setSuccessMessage(null), 4000);
  }

  return (
    <div className="admin-requests-page">
      <div className="page-header">
        <h1>Benutzerverwaltung</h1>
        <button
          className="btn-primary"
          onClick={() => {
            setSuccessMessage(null);
            setShowCreate(true);
          }}
        >
          + Neuer Benutzer
        </button>
      </div>

      {successMessage && <p className="success-message">{successMessage}</p>}
      {isLoading && <p>Wird geladen…</p>}
      {error && <p className="error">Fehler beim Laden der Benutzer.</p>}

      {!isLoading && !error && users?.length === 0 && (
        <p className="empty-state">Keine Benutzer vorhanden.</p>
      )}

      {users && users.length > 0 && (
        <div className="table-wrapper">
          <table className="requests-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>E-Mail</th>
                <th>Rolle</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>{ROLE_LABELS[u.role] ?? u.role}</td>
                  <td>
                    <span
                      className={`status-badge status-badge--${u.active ? "approved" : "rejected"}`}
                    >
                      {u.active ? "Aktiv" : "Inaktiv"}
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn-link"
                      onClick={() => setEditUser(u)}
                    >
                      Bearbeiten
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <CreateUserModal
          onClose={() => setShowCreate(false)}
          onSuccess={() => handleSuccess("Benutzer wurde angelegt.")}
        />
      )}

      {editUser && (
        <EditUserModal
          user={editUser}
          onClose={() => setEditUser(null)}
          onSuccess={() => handleSuccess("Benutzer wurde gespeichert.")}
        />
      )}
    </div>
  );
}
