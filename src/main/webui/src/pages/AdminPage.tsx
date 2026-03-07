import { useState, useEffect } from 'react';
import { adminService } from '../services/adminService';
import type { User, AppConfig, CreateUserRequest, UpdateUserAdminRequest, UserRole } from '../types';
import { getErrorMessage } from '../utils/errorUtils';


const ALL_ROLES: { value: UserRole; label: string }[] = [
  { value: 'ADMIN',         label: '🔑 Administrateur' },
  { value: 'DIVE_DIRECTOR', label: '🤿 Directeur de plongée' },
  { value: 'DIVER',         label: '🏊 Plongeur' },
];

const EMPTY_FORM: CreateUserRequest = {
  email: '', name: '', password: '', phone: '', roles: ['DIVER'],
};

export function AdminPage() {
  const [users, setUsers]                   = useState<User[]>([]);
  const [config, setConfig]                 = useState<AppConfig | null>(null);
  const [newMax, setNewMax]                 = useState('');
  const [newSiteName, setNewSiteName]       = useState('');
  const [msg, setMsg]                       = useState('');
  const [error, setError]                   = useState('');
  const [loading, setLoading]               = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createForm, setCreateForm]         = useState<CreateUserRequest>(EMPTY_FORM);
  const [createError, setCreateError]       = useState('');
  const [createLoading, setCreateLoading]   = useState(false);
  const [editingUserId, setEditingUserId]   = useState<number | null>(null);
  const [editForm, setEditForm]             = useState<UpdateUserAdminRequest>({ email: '', name: '', phone: '' });
  const [editError, setEditError]           = useState('');
  const [editLoading, setEditLoading]       = useState(false);

  // Listes configurables
  const [slotTypesText, setSlotTypesText] = useState('');
  const [clubsText, setClubsText]         = useState('');
  const [listLoading, setListLoading]     = useState(false);

  const loadData = async () => {
    try {
      const [u, c] = await Promise.all([adminService.getAllUsers(), adminService.getConfig()]);
      setUsers(u);
      setConfig(c);
      setNewMax(String(c.maxDivers));
      setNewSiteName(c.siteName ?? '');
      setSlotTypesText((c.slotTypes ?? []).join('\n'));
      setClubsText((c.clubs ?? []).join('\n'));
    } catch {
      setError('Erreur lors du chargement des données');
    }
  };
  useEffect(() => { loadData(); }, []);

  const handleUpdateMaxDivers = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setLoading(true);
    try {
      const updated = await adminService.updateMaxDivers(Number(newMax));
      setConfig(updated);
      setMsg(`Capacité maximale mise à jour : ${updated.maxDivers} plongeurs`);
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    } finally { setLoading(false); }
  };

  const handleUpdateSiteName = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setLoading(true);
    try {
      const updated = await adminService.updateSiteName(newSiteName);
      setConfig(updated);
      setMsg(`Nom du site mis à jour : "${updated.siteName}"`);
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    } finally { setLoading(false); }
  };

  /** Basculer un rôle dans la liste d'un utilisateur */
  const handleRoleToggle = async (user: User, role: UserRole) => {
    const current = user.roles ?? [user.role];
    const next = current.includes(role)
      ? current.filter(r => r !== role)
      : [...current, role];
    if (next.length === 0) { setError('Un utilisateur doit avoir au moins un rôle'); return; }
    try {
      const updated = await adminService.updateRoles(user.id, next);
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      setMsg(`Rôles de ${user.name} mis à jour`);
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    }
  };

  const handleDeleteUser = async (userId: number, name: string) => {
    if (!confirm(`Supprimer l'utilisateur "${name}" ?`)) return;
    try {
      await adminService.deleteUser(userId);
      setUsers(users.filter(u => u.id !== userId));
      setMsg('Utilisateur supprimé');
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    }
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault(); setCreateError(''); setCreateLoading(true);
    try {
      const created = await adminService.createUser(createForm);
      setUsers(prev => [...prev, created]);
      setCreateForm(EMPTY_FORM);
      setShowCreateForm(false);
      setMsg(`Utilisateur "${created.name}" créé avec succès`);
    } catch (err: unknown) {
      setCreateError(getErrorMessage(err));
    } finally { setCreateLoading(false); }
  };

  const handleUpdateSlotTypes = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setListLoading(true);
    const items = slotTypesText.split('\n').map(s => s.trim()).filter(Boolean);
    try {
      const updated = await adminService.updateSlotTypes(items);
      setConfig(updated);
      setSlotTypesText((updated.slotTypes ?? []).join('\n'));
      setMsg('Types de créneaux mis à jour');
    } catch (err: unknown) { setError(getErrorMessage(err)); }
    finally { setListLoading(false); }
  };

  const handleUpdateClubs = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setListLoading(true);
    const items = clubsText.split('\n').map(s => s.trim()).filter(Boolean);
    try {
      const updated = await adminService.updateClubs(items);
      setConfig(updated);
      setClubsText((updated.clubs ?? []).join('\n'));
      setMsg('Clubs mis à jour');
    } catch (err: unknown) { setError(getErrorMessage(err)); }
    finally { setListLoading(false); }
  };

  const toggleCreateRole = (role: UserRole) => {
    setCreateForm(f => {
      const cur = f.roles ?? [];
      const next = cur.includes(role) ? cur.filter(r => r !== role) : [...cur, role];
      return { ...f, roles: next.length ? next : [role] };
    });
  };

  const startEditUser = (user: User) => {
    setShowCreateForm(false);
    setEditingUserId(user.id);
    setEditForm({ email: user.email, name: user.name, phone: user.phone ?? '' });
    setEditError('');
  };

  const cancelEditUser = () => {
    setEditingUserId(null);
    setEditForm({ email: '', name: '', phone: '' });
    setEditError('');
  };

  const handleUpdateUser = async (e: React.FormEvent) => {
    e.preventDefault(); setEditError(''); setEditLoading(true);
    try {
      const updated = await adminService.updateUser(editingUserId!, editForm);
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      setMsg(`Utilisateur "${updated.name}" mis à jour`);
      cancelEditUser();
    } catch (err: unknown) {
      setEditError(getErrorMessage(err));
    } finally { setEditLoading(false); }
  };

  return (
    <div className="page">
      <h1>⚙️ Administration</h1>
      {msg   && <div className="alert alert-success">{msg}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Configuration du lac */}
      <div className="admin-section">
        <h2>🌊 Configuration du lac</h2>
        {config && (
          <div className="config-grid">
            <div className="config-item" style={{ gridColumn: 'span 4' }}>
              <span>Nom du site</span><strong style={{ fontSize: 16 }}>{config.siteName}</strong>
            </div>
            <div className="config-item"><span>Durée minimale</span><strong>{config.slotMinHours}h</strong></div>
            <div className="config-item"><span>Durée maximale</span><strong>{config.slotMaxHours}h</strong></div>
            <div className="config-item"><span>Résolution</span><strong>{config.slotResolutionMinutes} min</strong></div>
            <div className="config-item highlight"><span>Plongeurs max</span><strong>{config.maxDivers}</strong></div>
          </div>
        )}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
          <form onSubmit={handleUpdateSiteName} className="form form-inline" style={{ flex: 1, minWidth: 260 }}>
            <div className="form-group">
              <label>Nom du site</label>
              <input type="text" value={newSiteName} maxLength={100}
                onChange={e => setNewSiteName(e.target.value)} required placeholder="Ex: Carrière de Saint-Lin" />
            </div>
            <button type="submit" className="btn btn-outline" disabled={loading}>{loading ? '...' : 'Renommer'}</button>
          </form>
          <form onSubmit={handleUpdateMaxDivers} className="form form-inline" style={{ flex: 1, minWidth: 260 }}>
            <div className="form-group">
              <label>Maximum de plongeurs</label>
              <input type="number" min={1} max={500} value={newMax}
                onChange={e => setNewMax(e.target.value)} required />
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>{loading ? '...' : 'Mettre à jour'}</button>
          </form>
        </div>
      </div>

      {/* Listes configurables */}
      <div className="admin-section">
        <h2>📋 Listes configurables</h2>
        <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
          Saisissez un élément par ligne. Ces listes apparaissent lors de la création et modification d'un créneau.
        </p>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 24 }}>
          <form onSubmit={handleUpdateSlotTypes} style={{ flex: 1, minWidth: 280 }}>
            <div className="form-group">
              <label style={{ fontWeight: 700 }}>Types de créneaux</label>
              <textarea rows={8} value={slotTypesText}
                onChange={e => setSlotTypesText(e.target.value)}
                placeholder={"Club - Plongée\nClub - Apnée\nCODEP - Plongée\n..."}
                style={{ fontFamily: 'monospace', fontSize: 13 }} />
            </div>
            <button type="submit" className="btn btn-primary" disabled={listLoading}>
              {listLoading ? '...' : '💾 Enregistrer les types'}
            </button>
          </form>
          <form onSubmit={handleUpdateClubs} style={{ flex: 1, minWidth: 280 }}>
            <div className="form-group">
              <label style={{ fontWeight: 700 }}>Clubs</label>
              <textarea rows={8} value={clubsText}
                onChange={e => setClubsText(e.target.value)}
                placeholder={"Club Aqua Sport\nPlongeurs de la Côte\n..."}
                style={{ fontFamily: 'monospace', fontSize: 13 }} />
            </div>
            <button type="submit" className="btn btn-primary" disabled={listLoading}>
              {listLoading ? '...' : '💾 Enregistrer les clubs'}
            </button>
          </form>
        </div>
      </div>

      {/* Gestion des utilisateurs */}
      <div className="admin-section">        <div className="admin-section-header">
          <h2>👥 Gestion des utilisateurs ({users.length})</h2>
          <button className="btn btn-primary"
            onClick={() => { setShowCreateForm(v => !v); setCreateError(''); }}>
            {showCreateForm ? '✕ Annuler' : '+ Nouvel utilisateur'}
          </button>
        </div>

        {/* Formulaire création */}
        {showCreateForm && (
          <form onSubmit={handleCreateUser} className="create-user-form">
            <h3>Créer un utilisateur</h3>
            {createError && <div className="alert alert-error">{createError}</div>}
            <div className="form-row">
              <div className="form-group">
                <label>Nom complet *</label>
                <input placeholder="Jean Dupont" value={createForm.name}
                  onChange={e => setCreateForm(f => ({ ...f, name: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>Email *</label>
                <input type="email" placeholder="jean@example.com" value={createForm.email}
                  onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))} required />
              </div>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label>Téléphone</label>
                <input type="tel" placeholder="+33 6 12 34 56 78" value={createForm.phone ?? ''}
                  onChange={e => setCreateForm(f => ({ ...f, phone: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Mot de passe *</label>
                <input type="password" placeholder="Min. 6 caractères" value={createForm.password}
                  onChange={e => setCreateForm(f => ({ ...f, password: e.target.value }))}
                  required minLength={6} />
              </div>
            </div>
            <div className="form-group">
              <label>Rôles * (au moins un)</label>
              <div className="roles-checkboxes">
                {ALL_ROLES.map(r => (
                  <label key={r.value} className="role-checkbox-label">
                    <input type="checkbox"
                      checked={(createForm.roles ?? []).includes(r.value)}
                      onChange={() => toggleCreateRole(r.value)} />
                    {r.label}
                  </label>
                ))}
              </div>
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-outline"
                onClick={() => { setShowCreateForm(false); setCreateForm(EMPTY_FORM); }}>Annuler</button>
              <button type="submit" className="btn btn-primary" disabled={createLoading}>
                {createLoading ? 'Création...' : "✓ Créer l'utilisateur"}
              </button>
            </div>
          </form>
        )}

        {/* Formulaire modification */}
        {editingUserId !== null && (
          <form onSubmit={handleUpdateUser} className="create-user-form">
            <h3>Modifier l'utilisateur</h3>
            {editError && <div className="alert alert-error">{editError}</div>}
            <div className="form-row">
              <div className="form-group">
                <label>Nom complet *</label>
                <input placeholder="Jean Dupont" value={editForm.name}
                  onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>Email *</label>
                <input type="email" placeholder="jean@example.com" value={editForm.email}
                  onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} required />
              </div>
            </div>
            <div className="form-group">
              <label>Téléphone</label>
              <input type="tel" placeholder="+33 6 12 34 56 78" value={editForm.phone ?? ''}
                onChange={e => setEditForm(f => ({ ...f, phone: e.target.value }))} />
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-outline" onClick={cancelEditUser}>Annuler</button>
              <button type="submit" className="btn btn-primary" disabled={editLoading}>
                {editLoading ? 'Mise à jour...' : '✓ Enregistrer'}
              </button>
            </div>
          </form>
        )}

        {/* Table des utilisateurs */}
        <div className="users-table-wrapper">
          <table className="users-table">
            <thead>
              <tr>
                <th>Nom</th><th>Email</th><th>Téléphone</th>
                <th>Rôles</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => {
                const userRoles = u.roles ?? [u.role];
                return (
                  <tr key={u.id}>
                    <td><strong>{u.name}</strong></td>
                    <td>{u.email}</td>
                    <td>{u.phone || <span style={{ color: '#9ca3af' }}>—</span>}</td>
                    <td>
                      <div className="roles-checkboxes-inline">
                        {ALL_ROLES.map(r => (
                          <label key={r.value} className="role-checkbox-label small"
                            title={`${userRoles.includes(r.value) ? 'Retirer' : 'Ajouter'} le rôle ${r.label}`}>
                            <input type="checkbox"
                              checked={userRoles.includes(r.value)}
                              onChange={() => handleRoleToggle(u, r.value)} />
                            <span>{r.label}</span>
                          </label>
                        ))}
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-small"
                          style={{ color: '#2563eb', background: '#dbeafe', border: 'none' }}
                          onClick={() => startEditUser(u)}
                          title="Modifier">✏️ Modifier</button>
                        <button className="btn btn-small"
                          style={{ color: '#dc2626', background: '#fee2e2', border: 'none' }}
                          onClick={() => handleDeleteUser(u.id, u.name)}
                          title="Supprimer">🗑 Supprimer</button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

