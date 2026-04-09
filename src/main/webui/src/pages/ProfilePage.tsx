import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';

export function ProfilePage() {
  const { user } = useAuth();
  const [email, setEmail]             = useState(user?.email || '');
  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName]   = useState(user?.lastName  || '');
  const [phone, setPhone]         = useState(user?.phone || '');
  const [licenseNumber, setLicenseNumber] = useState(user?.licenseNumber || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Charge le profil complet depuis l'API pour avoir phone & licenseNumber à jour
  useEffect(() => {
    authService.getProfile().then(profile => {
      setEmail(profile.email || '');
      setFirstName(profile.firstName || '');
      setLastName(profile.lastName   || '');
      setPhone(profile.phone || '');
      setLicenseNumber(profile.licenseNumber || '');
    }).catch(() => {
      // Repli sur les données du contexte si l'API échoue
      setEmail(user?.email || '');
      setFirstName(user?.firstName || '');
      setLastName(user?.lastName   || '');
      setPhone(user?.phone || '');
      setLicenseNumber(user?.licenseNumber || '');
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const ROLE_LABELS: Record<string, string> = {
    ADMIN: '🔑 Administrateur',
    DIVE_DIRECTOR: '🤿 Directeur de plongée',
    GUEST: '👁️ Invité',
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setMsg(''); setError(''); setLoading(true);
    try {
      const updated = await authService.updateProfile(email, firstName, lastName, phone || undefined, licenseNumber || undefined);
      setMsg('Profil mis à jour avec succès !');
      if (user) {
        const stored = { ...user, ...updated, email: updated.email || email };
        localStorage.setItem('user', JSON.stringify(stored));
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
            {user.phone && <p className="profile-email">📞 {user.phone}</p>}
            {user.licenseNumber && <p className="profile-email">🏅 Licence : {user.licenseNumber}</p>}
            <span className="role-badge-large">{ROLE_LABELS[user.role]}</span>
          </div>
        </div>

        {msg && <div className="alert alert-success">{msg}</div>}
        {error && <div className="alert alert-error">{error}</div>}

        <div className="profile-section">
          <h3>✏️ Modifier le profil</h3>
          <form onSubmit={handleUpdateProfile} className="form">
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} required placeholder="votre@email.com" />
            </div>
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
            <div className="form-group">
              <label>Téléphone <span style={{ fontWeight: 400, color: 'var(--gray-500)' }}>(optionnel)</span></label>
              <input
                type="tel"
                value={phone}
                onChange={e => setPhone(e.target.value)}
                placeholder="Ex : 0612345678 ou +33612345678"
              />
            </div>
            <div className="form-group">
              <label>N° de licence fédérale <span style={{ fontWeight: 400, color: 'var(--gray-500)' }}>(optionnel)</span></label>
              <input
                type="text"
                value={licenseNumber}
                onChange={e => setLicenseNumber(e.target.value)}
                placeholder="Ex : A-14-1223422222"
                maxLength={20}
              />
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

