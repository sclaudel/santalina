import { Fragment, useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import { adminService } from '../services/adminService';
import { freeSessionService } from '../services/freeSessionService';
import { RichTextEditor } from '../components/RichTextEditor';
import { DpOrganizerMailer } from '../utils/dpMailDefaults';
import type { FreeDiveSession, FreeSessionShare } from '../types';

interface ProfilePageProps {
  onNavigate?: (page: string) => void;
}

export function ProfilePage({ onNavigate }: ProfilePageProps = {}) {
  const { user } = useAuth();
  const notifSectionRef = useRef<HTMLDivElement>(null);
  const [freeSessions, setFreeSessions] = useState<FreeDiveSession[]>([]);
  const [freeSessionsLoading, setFreeSessionsLoading] = useState(false);
  const [sharedSessions, setSharedSessions] = useState<FreeDiveSession[]>([]);
  const [sharedSessionsLoading, setSharedSessionsLoading] = useState(false);
  const [showNewSessionModal, setShowNewSessionModal] = useState(false);
  const [newSessionLabel, setNewSessionLabel] = useState('');
  const [newSessionDate, setNewSessionDate] = useState('');
  const [newSessionTime, setNewSessionTime] = useState('');
  const [editSession, setEditSession] = useState<FreeDiveSession | null>(null);
  const [copySource, setCopySource] = useState<FreeDiveSession | null>(null);
  const [copyDate, setCopyDate] = useState('');
  const [copyTime, setCopyTime] = useState('');
  const [copyLabel, setCopyLabel] = useState('');
  // partage : état du panneau de gestion par session
  const [sharingSessionId, setSharingSessionId] = useState<number | null>(null);
  const [shares, setShares] = useState<FreeSessionShare[]>([]);
  const [shareSearchQuery, setShareSearchQuery] = useState('');
  const [shareSearchResults, setShareSearchResults] = useState<{ id: number; name: string; email: string }[]>([]);
  const [shareSearching, setShareSearching] = useState(false);
  const [shareLevel, setShareLevel] = useState<'READ' | 'WRITE'>('READ');
  const [sharingError, setSharingError] = useState('');
  const [fsError, setFsError] = useState('');
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
  const [dpTemplate, setDpTemplate]     = useState(DpOrganizerMailer.DEFAULT_TEMPLATE);
  const [dpTemplateKey, setDpTemplateKey] = useState(0);
  const [dpTemplateLoading, setDpTemplateLoading] = useState(false);

  // Charge les sessions libres pour les DP/ADMIN
  useEffect(() => {
    if (user?.role === 'DIVE_DIRECTOR' || user?.role === 'ADMIN') {
      setFreeSessionsLoading(true);
      freeSessionService.list().then(setFreeSessions).catch(() => {}).finally(() => setFreeSessionsLoading(false));
      setSharedSessionsLoading(true);
      freeSessionService.listShared().then(setSharedSessions).catch(() => {}).finally(() => setSharedSessionsLoading(false));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.role]);

  const handleCreateSession = async () => {
    if (!newSessionDate || !newSessionTime) return;
    setFsError('');
    try {
      const created = await freeSessionService.create(newSessionLabel.trim() || null, newSessionDate, newSessionTime);
      setFreeSessions(prev => [created, ...prev]);
      setShowNewSessionModal(false); setNewSessionLabel(''); setNewSessionDate(''); setNewSessionTime('');
    } catch { setFsError('Impossible de créer la session.'); }
  };

  const handleUpdateSession = async () => {
    if (!editSession || !editSession.diveDate || !editSession.startTime) return;
    setFsError('');
    try {
      const updated = await freeSessionService.update(editSession.id, editSession.label ?? null, editSession.diveDate, editSession.startTime);
      setFreeSessions(prev => prev.map(s => s.id === updated.id ? updated : s));
      setEditSession(null);
    } catch { setFsError('Impossible de modifier la session.'); }
  };

  const handleCopySession = async () => {
    if (!copySource || !copyDate || !copyTime) return;
    setFsError('');
    try {
      const created = await freeSessionService.copy(copySource.id, copyLabel.trim() || null, copyDate, copyTime);
      setFreeSessions(prev => [created, ...prev]);
      setCopySource(null); setCopyDate(''); setCopyTime(''); setCopyLabel('');
    } catch { setFsError('Impossible de copier la session.'); }
  };

  const handleDeleteSession = async (id: number) => {
    if (!window.confirm('Supprimer cette organisation ? Toutes les données associées seront perdues.')) return;
    try {
      await freeSessionService.delete(id);
      setFreeSessions(prev => prev.filter(s => s.id !== id));
    } catch { setFsError('Impossible de supprimer la session.'); }
  };

  // ── Gestion du partage ──────────────────────────────────────────────────

  const openSharing = async (sessionId: number) => {
    setSharingSessionId(sessionId);
    setSharingError('');
    setShareSearchQuery('');
    setShareSearchResults([]);
    setShareLevel('READ');
    try {
      const list = await freeSessionService.listShares(sessionId);
      setShares(list);
    } catch { setSharingError('Impossible de charger les partages.'); }
  };

  const closeSharing = () => {
    setSharingSessionId(null);
    setShares([]);
    setShareSearchQuery('');
    setShareSearchResults([]);
    setSharingError('');
  };

  const handleShareSearch = async (q: string) => {
    setShareSearchQuery(q);
    if (!q.trim() || !sharingSessionId) { setShareSearchResults([]); return; }
    setShareSearching(true);
    try {
      const results = await freeSessionService.searchDp(sharingSessionId, q);
      setShareSearchResults(results);
    } catch { /* ignore */ } finally { setShareSearching(false); }
  };

  const handleAddShare = async (targetUserId: number) => {
    if (!sharingSessionId) return;
    setSharingError('');
    try {
      const share = await freeSessionService.shareWith(sharingSessionId, targetUserId, shareLevel);
      setShares(prev => [...prev, share]);
      setShareSearchQuery('');
      setShareSearchResults([]);
    } catch (err: unknown) {
      const m = (err as { response?: { data?: string } })?.response?.data;
      setSharingError(typeof m === 'string' ? m : 'Impossible de partager la session.');
    }
  };

  const handleUpdateShare = async (shareId: number, accessLevel: 'READ' | 'WRITE') => {
    if (!sharingSessionId) return;
    try {
      const updated = await freeSessionService.updateShare(sharingSessionId, shareId, accessLevel);
      setShares(prev => prev.map(s => s.id === shareId ? updated : s));
    } catch { setSharingError('Impossible de modifier le partage.'); }
  };

  const handleDeleteShare = async (shareId: number) => {
    if (!sharingSessionId) return;
    try {
      await freeSessionService.deleteShare(sharingSessionId, shareId);
      setShares(prev => prev.filter(s => s.id !== shareId));
    } catch { setSharingError('Impossible de révoquer le partage.'); }
  };

  const handleLeaveShare = async (sessionId: number) => {
    if (!window.confirm('Quitter cette session partagée ? Vous n\'y aurez plus accès.')) return;
    try {
      await freeSessionService.leaveShare(sessionId);
      setSharedSessions(prev => prev.filter(s => s.id !== sessionId));
    } catch { /* ignore */ }
  };

  const fmtDate = (d: string) => { const [y, m, day] = d.split('-'); return `${day}/${m}/${y}`; };

  // Scroll vers la section notifications si le hash #notifications est présent dans l'URL
  useEffect(() => {
    if (window.location.hash === '#notifications') {
      notifSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, []);

  // Charge le profil complet depuis l'API pour avoir phone & licenseNumber à jour
  useEffect(() => {
    Promise.all([
      authService.getProfile(),
      adminService.getConfig().catch(() => null),
    ]).then(([profile, cfg]) => {
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
      const adminTpl = cfg?.defaultOrganizerMailTemplate ?? '';
      const tpl = profile.dpOrganizerEmailTemplate || adminTpl || DpOrganizerMailer.DEFAULT_TEMPLATE;
      setDpTemplate(tpl);
      setDpTemplateKey(k => k + 1);
      setClubs(cfg?.clubs ?? []);
    }).catch(() => {
      // Repli sur les données du contexte si l'API échoue
      setFirstName(user?.firstName || '');
      setLastName(user?.lastName   || '');
      setPhone(user?.phone || '');
      setLicenseNumber(user?.licenseNumber || '');
      setClub(user?.club || '');
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

  const handleSaveDpTemplate = async () => {
    setMsg(''); setError(''); setDpTemplateLoading(true);
    try {
      await authService.updateDpEmailTemplate(dpTemplate);
      setMsg('Modèle de mail enregistré.');
    } catch {
      setError('Erreur lors de l\'enregistrement du modèle.');
    } finally {
      setDpTemplateLoading(false);
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

        {(user.role === 'DIVE_DIRECTOR' || user.role === 'ADMIN') && (
          <div className="profile-section">
            <h3>🧩 Organisations libres{freeSessions.length > 0 ? ` (${freeSessions.length}/15)` : ''} <span style={{ fontSize: 10, fontWeight: 600, background: '#f59e0b', color: '#fff', borderRadius: 4, padding: '1px 6px', verticalAlign: 'middle', letterSpacing: '0.05em' }}>BETA</span></h3>
            <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 12 }}>
              Organisez des palanquées sans créer de créneau. Jusqu'à 15 organisations au maximum.
            </p>
            {fsError && <div className="alert alert-error">{fsError}</div>}
            {freeSessionsLoading ? <p style={{ color: '#9ca3af' }}>Chargement…</p> : (
              <>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 12 }}>
                  {freeSessions.length === 0 && <p style={{ color: '#9ca3af', fontSize: 13 }}>Aucune organisation. Créez-en une ci-dessous.</p>}
                  {freeSessions.map(s => (
                    editSession?.id === s.id ? (
                      <div key={s.id} style={{ background: '#f9fafb', borderRadius: 8, padding: '10px 12px', border: '1px solid #e5e7eb', display: 'flex', flexDirection: 'column', gap: 8 }}>
                        <div className="form-row">
                          <div className="form-group" style={{ flex: 1 }}>
                            <label>Libellé</label>
                            <input value={editSession.label ?? ''} onChange={e => setEditSession(es => es ? { ...es, label: e.target.value } : es)} placeholder="Optionnel" />
                          </div>
                          <div className="form-group"><label>Date *</label><input type="date" value={editSession.diveDate} onChange={e => setEditSession(es => es ? { ...es, diveDate: e.target.value } : es)} required /></div>
                          <div className="form-group"><label>Heure *</label><input type="time" value={editSession.startTime.slice(0,5)} onChange={e => setEditSession(es => es ? { ...es, startTime: e.target.value } : es)} required /></div>
                        </div>
                        <div style={{ display: 'flex', gap: 8 }}>
                          <button className="btn btn-primary" style={{ padding: '4px 12px', fontSize: 13 }} onClick={handleUpdateSession}>💾 Enregistrer</button>
                          <button className="btn btn-secondary" style={{ padding: '4px 12px', fontSize: 13 }} onClick={() => setEditSession(null)}>Annuler</button>
                        </div>
                      </div>
                    ) : (
                      <Fragment key={s.id}>
                      <div style={{ background: '#f9fafb', borderRadius: 8, padding: '8px 12px', border: '1px solid #e5e7eb', display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                        <div style={{ flex: '1 1 160px', minWidth: 0 }}>
                          <span style={{ fontWeight: 600 }}>{fmtDate(s.diveDate)}</span>
                          <span style={{ margin: '0 6px', color: '#9ca3af' }}>·</span>
                          <span>{s.startTime.slice(0,5)}</span>
                          {s.label && <><span style={{ margin: '0 6px', color: '#9ca3af' }}>·</span><span style={{ color: '#374151', wordBreak: 'break-word' }}>{s.label}</span></>}
                        </div>
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', flexShrink: 0 }}>
                          <button className="btn btn-primary" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => onNavigate?.(`free-session-${s.id}`)}>🧩 Ouvrir</button>
                          <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12 }} title="Partager avec un autre DP" onClick={() => sharingSessionId === s.id ? closeSharing() : openSharing(s.id)}>🔗</button>
                          <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12 }} title="Copier (garder les plongeurs)" onClick={() => { setCopySource(s); setCopyLabel(s.label ?? ''); setCopyDate(''); setCopyTime(''); setFsError(''); }}>📋</button>
                          <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => setEditSession(s)}>✏️</button>
                          <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12, color: '#ef4444' }} onClick={() => handleDeleteSession(s.id)}>🗑️</button>
                        </div>
                      </div>
                      {/* Panneau de gestion des partages */}
                      {sharingSessionId === s.id && (
                        <div style={{ background: '#fafaf9', border: '1px solid #d1fae5', borderRadius: 8, padding: 12, marginTop: 4 }}>
                          <p style={{ fontWeight: 600, fontSize: 13, marginBottom: 8 }}>🔗 Partager « {s.label ?? fmtDate(s.diveDate)} »</p>
                          {sharingError && <div className="alert alert-error" style={{ marginBottom: 8 }}>{sharingError}</div>}
                          {/* Partages existants */}
                          {shares.length > 0 && (
                            <div style={{ marginBottom: 10 }}>
                              {shares.map(sh => (
                                <div key={sh.id} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                                  <span style={{ flex: 1, fontSize: 13 }}>{sh.sharedWithName} <span style={{ color: '#6b7280', fontSize: 11 }}>({sh.sharedWithEmail})</span></span>
                                  <select
                                    value={sh.accessLevel}
                                    style={{ fontSize: 12, padding: '2px 6px' }}
                                    onChange={e => handleUpdateShare(sh.id, e.target.value as 'READ' | 'WRITE')}
                                  >
                                    <option value="READ">Lecture</option>
                                    <option value="WRITE">Écriture</option>
                                  </select>
                                  <button className="btn btn-secondary" style={{ padding: '2px 8px', fontSize: 12, color: '#ef4444' }} onClick={() => handleDeleteShare(sh.id)}>✕</button>
                                </div>
                              ))}
                            </div>
                          )}
                          {shares.length === 0 && <p style={{ color: '#9ca3af', fontSize: 12, marginBottom: 8 }}>Pas encore de partage.</p>}
                          {/* Recherche de DP */}
                          <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
                            <input
                              style={{ flex: 1, minWidth: 140, fontSize: 13 }}
                              placeholder="Rechercher un DP (nom, e-mail…)"
                              value={shareSearchQuery}
                              onChange={e => handleShareSearch(e.target.value)}
                            />
                            <select value={shareLevel} onChange={e => setShareLevel(e.target.value as 'READ' | 'WRITE')} style={{ fontSize: 12, padding: '4px 6px' }}>
                              <option value="READ">Lecture</option>
                              <option value="WRITE">Écriture</option>
                            </select>
                          </div>
                          {shareSearching && <p style={{ fontSize: 12, color: '#9ca3af', marginTop: 4 }}>Recherche…</p>}
                          {shareSearchResults.length > 0 && (
                            <div style={{ border: '1px solid #e5e7eb', borderRadius: 6, marginTop: 4, background: '#fff' }}>
                              {shareSearchResults.map(r => (
                                <div key={r.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 10px', borderBottom: '1px solid #f3f4f6' }}>
                                  <span style={{ flex: 1, fontSize: 13 }}>{r.name} <span style={{ color: '#6b7280', fontSize: 11 }}>({r.email})</span></span>
                                  <button className="btn btn-primary" style={{ padding: '2px 10px', fontSize: 12 }} onClick={() => handleAddShare(r.id)}>+ Partager</button>
                                </div>
                              ))}
                            </div>
                          )}
                          <button className="btn btn-secondary" style={{ marginTop: 8, padding: '3px 10px', fontSize: 12 }} onClick={closeSharing}>Fermer</button>
                        </div>
                      )}
                      </Fragment>
                    )
                  ))}
                </div>
                {/* Modal copie */}
                {copySource && (
                  <div style={{ background: '#f0fdf4', borderRadius: 8, padding: 12, border: '1px solid #bbf7d0', marginBottom: 4 }}>
                    <p style={{ fontWeight: 600, fontSize: 13, marginBottom: 8 }}>📋 Copier « {copySource.label ?? fmtDate(copySource.diveDate)} » — choisissez la nouvelle date</p>
                    <div className="form-row">
                      <div className="form-group" style={{ flex: 1 }}>
                        <label>Libellé <span style={{ fontWeight: 400, color: '#6b7280' }}>(optionnel)</span></label>
                        <input value={copyLabel} onChange={e => setCopyLabel(e.target.value)} placeholder="Ex : Sortie club…" />
                      </div>
                      <div className="form-group"><label>Date *</label><input type="date" value={copyDate} onChange={e => setCopyDate(e.target.value)} required /></div>
                      <div className="form-group"><label>Heure *</label><input type="time" value={copyTime} onChange={e => setCopyTime(e.target.value)} required /></div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                      <button className="btn btn-primary" onClick={handleCopySession} disabled={!copyDate || !copyTime}>📋 Copier</button>
                      <button className="btn btn-secondary" onClick={() => { setCopySource(null); setCopyDate(''); setCopyTime(''); setCopyLabel(''); }}>Annuler</button>
                    </div>
                  </div>
                )}

                {freeSessions.length < 15 && !showNewSessionModal && (
                  <button className="btn btn-primary" style={{ width: '100%' }} onClick={() => { setShowNewSessionModal(true); setFsError(''); }}>+ Nouvelle organisation</button>
                )}
                {showNewSessionModal && (
                  <div style={{ background: '#f0f9ff', borderRadius: 8, padding: 12, border: '1px solid #bae6fd' }}>
                    <div className="form-row">
                      <div className="form-group" style={{ flex: 1 }}>
                        <label>Libellé <span style={{ fontWeight: 400, color: '#6b7280' }}>(optionnel)</span></label>
                        <input value={newSessionLabel} onChange={e => setNewSessionLabel(e.target.value)} placeholder="Ex : Sortie club, Lac du Bourget…" />
                      </div>
                      <div className="form-group"><label>Date *</label><input type="date" value={newSessionDate} onChange={e => setNewSessionDate(e.target.value)} required /></div>
                      <div className="form-group"><label>Heure *</label><input type="time" value={newSessionTime} onChange={e => setNewSessionTime(e.target.value)} required /></div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                      <button className="btn btn-primary" onClick={handleCreateSession} disabled={!newSessionDate || !newSessionTime}>💾 Créer</button>
                      <button className="btn btn-secondary" onClick={() => { setShowNewSessionModal(false); setNewSessionLabel(''); setNewSessionDate(''); setNewSessionTime(''); }}>Annuler</button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* Section : sessions partagées avec moi */}
        {(user.role === 'DIVE_DIRECTOR' || user.role === 'ADMIN') && (sharedSessionsLoading || sharedSessions.length > 0) && (
          <div className="profile-section">
            <h3>🔗 Partagées avec moi</h3>
            <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 12 }}>
              Organisations libres que d'autres directeurs de plongée ont partagées avec vous.
              Elles ne comptent pas dans votre quota.
            </p>
            {sharedSessionsLoading ? <p style={{ color: '#9ca3af' }}>Chargement…</p> : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {sharedSessions.map(s => (
                  <div key={s.id} style={{ background: '#f9fafb', borderRadius: 8, padding: '8px 12px', border: '1px solid #e5e7eb', display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                    <div style={{ flex: '1 1 160px', minWidth: 0 }}>
                      <span style={{ fontWeight: 600 }}>{fmtDate(s.diveDate)}</span>
                      <span style={{ margin: '0 6px', color: '#9ca3af' }}>·</span>
                      <span>{s.startTime.slice(0, 5)}</span>
                      {s.label && <><span style={{ margin: '0 6px', color: '#9ca3af' }}>·</span><span style={{ color: '#374151', wordBreak: 'break-word' }}>{s.label}</span></>}
                      {s.ownerName && <><span style={{ margin: '0 6px', color: '#9ca3af' }}>·</span><span style={{ color: '#6b7280', fontSize: 12 }}>par {s.ownerName}</span></>}
                      <span style={{ marginLeft: 6, fontSize: 11, fontWeight: 600, background: s.accessLevel === 'WRITE' ? '#d1fae5' : '#e0e7ff', color: s.accessLevel === 'WRITE' ? '#065f46' : '#3730a3', borderRadius: 4, padding: '1px 6px' }}>
                        {s.accessLevel === 'WRITE' ? 'Écriture' : 'Lecture'}
                      </span>
                    </div>
                    <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                      <button className="btn btn-primary" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => onNavigate?.(`free-session-${s.id}`)}>🧩 Ouvrir</button>
                      <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12, color: '#ef4444' }} title="Quitter cette session" onClick={() => handleLeaveShare(s.id)}>Quitter</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {(user.role === 'DIVE_DIRECTOR' || user.role === 'ADMIN') && (
          <div className="profile-section">
            <h3>📧 Modèle d'e-mail d'organisation</h3>
            <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 8 }}>
              Ce modèle est pré-chargé dans l'éditeur lorsque vous envoyez un mail
              d'organisation depuis la page Palanquées.{' '}
              <br />Variables disponibles :
              {['{siteName}', '{slotDate}', '{startTime}', '{endTime}', '{slotTitle}',
                '{dpName}', '{dpEmail}', '{dpPhone}'].map(v => (
                <code key={v} style={{ margin: '0 3px', background: '#e0e7ff', borderRadius: 3, padding: '1px 4px', fontSize: 11 }}>{v}</code>
              ))}
            </p>
            <RichTextEditor
              key={dpTemplateKey}
              initialValue={dpTemplate}
              onChange={setDpTemplate}
              minHeight={400}
            />
            <div style={{ marginTop: 10 }}>
              <button className="btn btn-primary" onClick={handleSaveDpTemplate} disabled={dpTemplateLoading}>
                {dpTemplateLoading ? '...' : '💾 Enregistrer le modèle'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

