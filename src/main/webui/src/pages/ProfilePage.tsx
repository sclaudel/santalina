import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import { adminService } from '../services/adminService';

export function ProfilePage() {
  const { user } = useAuth();
  const notifSectionRef = useRef<HTMLDivElement>(null);
  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName]   = useState(user?.lastName  || '');
  const [phone, setPhone]         = useState(user?.phone || '');
  const [licenseNumber, setLicenseNumber] = useState(user?.licenseNumber || '');
  const [club, setClub]           = useState(user?.club || '');
  const [clubs, setClubs]         = useState<string[]>([]);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [notifOnRegistration, setNotifOnRegistration] = useState(user?.notifOnRegistration ?? true);
  const [notifOnApproved, setNotifOnApproved] = useState(user?.notifOnApproved ?? true);
  const [notifOnCancelled, setNotifOnCancelled] = useState(user?.notifOnCancelled ?? true);
  const [notifOnMovedToWaitlist, setNotifOnMovedToWaitlist] = useState(user?.notifOnMovedToWaitlist ?? true);
  const [notifOnDpRegistration, setNotifOnDpRegistration] = useState(user?.notifOnDpRegistration ?? true);
  const [notifOnCreatorRegistration, setNotifOnCreatorRegistration] = useState(user?.notifOnCreatorRegistration ?? false);
  const [notifOnSafetyReminder, setNotifOnSafetyReminder] = useState(user?.notifOnSafetyReminder ?? true);
  const [notifLoading, setNotifLoading] = useState(false);

  // Scroll vers la section notifications si le hash #notifications est présent dans l'URL
  useEffect(() => {
    if (window.location.hash === '#notifications') {
      notifSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, []);

  // Charge le profil complet depuis l'API pour avoir phone & licenseNumber à jour
  useEffect(() => {
    authService.getProfile().then(profile => {
      setFirstName(profile.firstName || '');
      setLastName(profile.lastName   || '');
      setPhone(profile.phone || '');
      setLicenseNumber(profile.licenseNumber || '');
      setClub(profile.club || '');
      setNotifOnRegistration(profile.notifOnRegistration ?? true);
      setNotifOnApproved(profile.notifOnApproved ?? true);
      setNotifOnCancelled(profile.notifOnCancelled ?? true);
      setNotifOnMovedToWaitlist(profile.notifOnMovedToWaitlist ?? true);
      setNotifOnDpRegistration(profile.notifOnDpRegistration ?? true);
      setNotifOnCreatorRegistration(profile.notifOnCreatorRegistration ?? false);
      setNotifOnSafetyReminder(profile.notifOnSafetyReminder ?? true);
    }).catch(() => {
      // Repli sur les données du contexte si l'API échoue
      setFirstName(user?.firstName || '');
      setLastName(user?.lastName   || '');
      setPhone(user?.phone || '');
      setLicenseNumber(user?.licenseNumber || '');
      setClub(user?.club || '');
    });
    adminService.getConfig().then(cfg => setClubs(cfg.clubs ?? [])).catch(() => {});
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
      const updated = await authService.updateProfile(firstName, lastName, phone || undefined, licenseNumber || undefined, club || undefined);
      setMsg('Profil mis à jour avec succès !');
      if (user) {
        const stored = { ...user, ...updated };
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

  const handleUpdateNotifPrefs = async () => {
    setMsg(''); setError(''); setNotifLoading(true);
    try {
      await authService.updateNotifPrefs({ notifOnRegistration, notifOnApproved, notifOnCancelled, notifOnMovedToWaitlist, notifOnDpRegistration, notifOnCreatorRegistration, notifOnSafetyReminder });
      setMsg('Préférences de notification enregistrées.');
    } catch {
      setError('Erreur lors de l\'enregistrement des préférences.');
    } finally {
      setNotifLoading(false);
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
            {user.club && <p className="profile-email">🤿 Club : {user.club}</p>}
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
            <div className="form-group">
              <label>Club d'appartenance <span style={{ fontWeight: 400, color: 'var(--gray-500)' }}>(optionnel)</span></label>
              <select value={club} onChange={e => setClub(e.target.value)}>
                <option value="">— Aucun / Non affilié —</option>
                {clubs.map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Enregistrement...' : 'Enregistrer'}
            </button>
          </form>
        </div>

        <div className="profile-section">
          <h3>🔒 Changer le mot de passe</h3>
          <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 12 }}>Le nouveau mot de passe doit contenir au moins <strong>8 caractères</strong>, une <strong>majuscule</strong>, un <strong>chiffre</strong> et un <strong>caractère spécial</strong>.</p>
          <form onSubmit={handleChangePassword} className="form">
            <div className="form-group">
              <label>Mot de passe actuel</label>
              <input type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Nouveau mot de passe</label>
              <input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} required minLength={8}
                pattern="^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,100}$"
                title="Au moins 8 caractères, une majuscule, un chiffre et un caractère spécial" />
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Modification...' : 'Changer le mot de passe'}
            </button>
          </form>
        </div>

        <div className="profile-section" id="notifications" ref={notifSectionRef}>
          <h3>🔔 Notifications par e-mail</h3>
          <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
            Désactivez les types de notifications que vous ne souhaitez pas recevoir.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 20 }}>
            {([
              { label: '📩 Confirmation d\'inscription en liste d\'attente', value: notifOnRegistration, setter: setNotifOnRegistration },
              { label: '✅ Inscription validée par le directeur de plongée', value: notifOnApproved, setter: setNotifOnApproved },
              { label: '❌ Inscription annulée ou supprimée', value: notifOnCancelled, setter: setNotifOnCancelled },
              { label: '⏳ Remis en liste d\'attente', value: notifOnMovedToWaitlist, setter: setNotifOnMovedToWaitlist },
              { label: '📋 Nouvelles inscriptions sur mes créneaux (en tant que DP assigné)', value: notifOnDpRegistration, setter: setNotifOnDpRegistration },
              { label: '📋 Nouvelles inscriptions sur mes créneaux (en tant que créateur)', value: notifOnCreatorRegistration, setter: setNotifOnCreatorRegistration },
              { label: '📋 Rappel fiche de sécurité après la sortie (reçu en tant que DP assigné)', value: notifOnSafetyReminder, setter: setNotifOnSafetyReminder },
            ] as { label: string; value: boolean; setter: (v: boolean) => void }[]).map(({ label, value, setter }) => (
              <label key={label} style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
                <input type="checkbox" checked={value} onChange={e => setter(e.target.checked)} />
                <span>{label}</span>
              </label>
            ))}
          </div>
          <button className="btn btn-primary" onClick={handleUpdateNotifPrefs} disabled={notifLoading}>
            {notifLoading ? '...' : '💾 Enregistrer'}
          </button>
        </div>
      </div>
    </div>
  );
}

