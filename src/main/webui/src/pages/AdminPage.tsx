import { useState, useEffect } from 'react';
import { adminService } from '../services/adminService';
import type { User, AppConfig } from '../types';
import type { UserRole } from '../types';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '🔑 Administrateur',
  DIVE_DIRECTOR: '🤿 Directeur de plongée',
  GUEST: '👁️ Invité',
};

export function AdminPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [config, setConfig] = useState<AppConfig | null>(null);
  const [newMax, setNewMax] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const loadData = async () => {
    try {
      const [u, c] = await Promise.all([adminService.getAllUsers(), adminService.getConfig()]);
      setUsers(u);
      setConfig(c);
      setNewMax(String(c.maxDivers));
    } catch {
      setError('Erreur lors du chargement des données');
    }
  };

  useEffect(() => { loadData(); }, []);

  const handleUpdateMaxDivers = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(''); setError(''); setLoading(true);
    try {
      const updated = await adminService.updateMaxDivers(Number(newMax));
      setConfig(updated);
      setMsg(`Capacité maximale mise à jour : ${updated.maxDivers} plongeurs`);
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(m || 'Erreur lors de la mise à jour');
    } finally {
      setLoading(false);
    }
  };

  const handleRoleChange = async (userId: number, role: UserRole) => {
    try {
      await adminService.updateRole(userId, role);
      setUsers(users.map(u => u.id === userId ? { ...u, role } : u));
      setMsg('Rôle mis à jour');
    } catch {
      setError('Erreur lors du changement de rôle');
    }
  };

  return (
    <div className="page">
      <h1>⚙️ Administration</h1>

      {msg && <div className="alert alert-success">{msg}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Configuration du lac */}
      <div className="admin-section">
        <h2>🌊 Configuration du lac</h2>
        {config && (
          <div className="config-grid">
            <div className="config-item">
              <span>Durée minimale</span>
              <strong>{config.slotMinHours}h</strong>
            </div>
            <div className="config-item">
              <span>Durée maximale</span>
              <strong>{config.slotMaxHours}h</strong>
            </div>
            <div className="config-item">
              <span>Résolution</span>
              <strong>{config.slotResolutionMinutes} min</strong>
            </div>
            <div className="config-item highlight">
              <span>Plongeurs max</span>
              <strong>{config.maxDivers}</strong>
            </div>
          </div>
        )}
        <form onSubmit={handleUpdateMaxDivers} className="form form-inline">
          <div className="form-group">
            <label>Nouveau maximum de plongeurs</label>
            <input
              type="number" min={1} max={500}
              value={newMax}
              onChange={e => setNewMax(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Mise à jour...' : 'Mettre à jour'}
          </button>
        </form>
      </div>

      {/* Gestion des utilisateurs */}
      <div className="admin-section">
        <h2>👥 Gestion des utilisateurs ({users.length})</h2>
        <div className="users-table-wrapper">
          <table className="users-table">
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Rôle</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td><strong>{u.name}</strong></td>
                  <td>{u.email}</td>
                  <td>
                    <span className={`role-tag role-${u.role.toLowerCase()}`}>
                      {ROLE_LABELS[u.role]}
                    </span>
                  </td>
                  <td>
                    <select
                      value={u.role}
                      onChange={e => handleRoleChange(u.id, e.target.value as UserRole)}
                      className="role-select"
                    >
                      <option value="GUEST">Invité</option>
                      <option value="DIVE_DIRECTOR">Directeur de plongée</option>
                      <option value="ADMIN">Administrateur</option>
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

