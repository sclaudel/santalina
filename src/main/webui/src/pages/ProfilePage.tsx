import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';

export function ProfilePage() {
  const { user } = useAuth();
  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName]   = useState(user?.lastName  || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setFirstName(user?.firstName || '');
    setLastName(user?.lastName  || '');
  }, [user]);

  const ROLE_LABELS: Record<string, string> = {
    ADMIN: '🔑 Administrateur',
    DIVE_DIRECTOR: '🤿 Directeur de plongée',
    GUEST: '👁️ Invité',
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(''); setError(''); setLoading(true);
    try {
      await authService.updateProfile(firstName, lastName);
      setMsg('Profil mis à jour avec succès !');
      if (user) {
        const updated = { ...user, firstName, lastName, name: `${firstName} ${lastName}`.trim() };
        localStorage.setItem('user', JSON.stringify(updated));
        window.location.reload();
      }
    } catch {
      setError('Erreur lors de la mise à jour du profil');
    } finally {
      setLoading(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(''); setError(''); setLoading(true);
    try {
      await authService.changePassword(currentPassword, newPassword);
      setMsg('Mot de passe modifié avec succès !');
      setCurrentPassword(''); setNewPassword('');
    } catch (err: unknown) {
      const m = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(m || 'Erreur lors du changement de mot de passe');
    } finally {
      setLoading(false);
    }
  };

  if (!user) return <div className="page"><p>Vous devez être connect��.</p></div>;

  return (
    <div className="page">
      <div className="profile-card">
        <div className="profile-header">
          <div className="profile-avatar">
            {(user.firstName || user.name).charAt(0).toUpperCase()}
          </div>
          <div>
            <h2>{user.firstName} {user.lastName}</h2>
            <p className="profile-email">📧 {user.email}</p>
            <span className="role-badge-large">{ROLE_LABELS[user.role]}</span>
          </div>
        </div>

        {msg && <div className="alert alert-success">{msg}</div>}
        {error && <div className="alert alert-error">{error}</div>}

        <div className="profile-section">
          <h3>✏️ Modifier le profil</h3>
          <form onSubmit={handleUpdateProfile} className="form">
            <div className="form-row">
              <div className="form-group" style={{ flex: 1 }}>
                <label>Prénom</label>
                <input type="text" value={firstName} onChange={e => setFirstName(e.target.value)} required minLength={2} />
              </div>
              <div className="form-group" style={{ flex: 1 }}>
                <label>Nom</label>
                <input type="text" value={lastName} onChange={e => setLastName(e.target.value)} required minLength={2} />
              </div>
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Enregistrement...' : 'Enregistrer'}
            </button>
          </form>
        </div>

        <div className="profile-section">
          <h3>🔒 Changer le mot de passe</h3>
          <form onSubmit={handleChangePassword} className="form">
            <div className="form-group">
              <label>Mot de passe actuel</label>
              <input type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Nouveau mot de passe</label>
              <input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} required minLength={6} />
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Modification...' : 'Changer le mot de passe'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

