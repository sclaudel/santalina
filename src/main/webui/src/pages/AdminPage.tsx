import { useState, useEffect, useRef } from 'react';
import { adminService } from '../services/adminService';
import type { User, AppConfig, CreateUserRequest, UpdateUserAdminRequest, UserRole, LogInfo, ImportResult, CsvImportResult } from '../types';
import { getErrorMessage } from '../utils/errorUtils';


const ALL_ROLES: { value: UserRole; label: string }[] = [
  { value: 'ADMIN',         label: '🔑 Administrateur' },
  { value: 'DIVE_DIRECTOR', label: '🤿 Directeur de plongée' },
  { value: 'DIVER',         label: '🏊 Plongeur' },
];

const EMPTY_FORM: CreateUserRequest = {
  email: '', firstName: '', lastName: '', password: '', phone: '', licenseNumber: '', club: '', roles: ['DIVER'],
};

type AdminTabId = 'general' | 'catalog' | 'users' | 'operations';

const ADMIN_TABS: { id: AdminTabId; label: string; description: string }[] = [
  { id: 'general', label: '⚙️ Général', description: 'Réglages principaux du site' },
  { id: 'catalog', label: '📚 Référentiels', description: 'Listes et types de créneaux' },
  { id: 'users', label: '👥 Utilisateurs', description: 'Comptes, rôles et permissions' },
  { id: 'operations', label: '🛠️ Opérations', description: 'Logs, sauvegarde et restauration' },
];

export function AdminPage() {
  const [users, setUsers]                   = useState<User[]>([]);
  const [config, setConfig]                 = useState<AppConfig | null>(null);
  const [newMax, setNewMax]                       = useState('');
  const [newSiteName, setNewSiteName]             = useState('');
  const [newDefaultSlotHours, setNewDefaultSlotHours] = useState('2');
  const [msg, setMsg]                       = useState('');
  const [error, setError]                   = useState('');
  const [loading, setLoading]               = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [createForm, setCreateForm]         = useState<CreateUserRequest>(EMPTY_FORM);
  const [createError, setCreateError]       = useState('');
  const [createLoading, setCreateLoading]   = useState(false);
  const [editingUserId, setEditingUserId]   = useState<number | null>(null);
  const [editForm, setEditForm]             = useState<UpdateUserAdminRequest>({ email: '', firstName: '', lastName: '', phone: '', licenseNumber: '', club: '' });
  const [editError, setEditError]           = useState('');
  const [editLoading, setEditLoading]       = useState(false);

  // Listes configurables
  const [slotTypesText, setSlotTypesText]   = useState('');
  const [exclusiveSlotTypes, setExclusiveSlotTypes] = useState<string[]>([]);
  const [clubsText, setClubsText]           = useState('');
  const [levelsText, setLevelsText]         = useState('');
  const [listLoading, setListLoading]       = useState(false);
  const [exclusiveLoading, setExclusiveLoading] = useState(false);

  // Heures de réservation
  const [bookingOpenHour, setBookingOpenHour]   = useState<number>(-1);
  const [bookingCloseHour, setBookingCloseHour] = useState<number>(-1);
  const [bookingHoursLoading, setBookingHoursLoading] = useState(false);

  // Email de notification de réservation
  const [notificationEmail, setNotificationEmail]       = useState('');
  const [notificationEmailLoading, setNotificationEmailLoading] = useState(false);

  // Paramètres de notification par type
  const [notifRegistration, setNotifRegistration]     = useState(true);
  const [notifApproved, setNotifApproved]             = useState(true);
  const [notifCancelled, setNotifCancelled]           = useState(true);
  const [notifMovedToWl, setNotifMovedToWl]           = useState(true);
  const [notifDpNewReg, setNotifDpNewReg]             = useState(true);
  const [notifSafetyReminder, setNotifSafetyReminder] = useState(false);
  const [safetyReminderDelayDays, setSafetyReminderDelayDays] = useState(3);
  const [safetyReminderEmailBody, setSafetyReminderEmailBody] = useState('');
  const [notifSettingsLoading, setNotifSettingsLoading] = useState(false);

  // Recherche et pagination utilisateurs
  const [userSearch, setUserSearch]   = useState('');
  const [userPage, setUserPage]       = useState(1);
  const USER_PAGE_SIZE = 10;

  // Nombre total d'administrateurs (permet de protéger le dernier)
  const adminCount = users.filter(u => (u.roles ?? [u.role]).includes('ADMIN')).length;

  // Durée de récurrence
  const [newMaxRecurringMonths, setNewMaxRecurringMonths] = useState('4');
  const [recurringLoading, setRecurringLoading] = useState(false);

  // Logs
  const [logs, setLogs]                 = useState<LogInfo[]>([]);
  const [logsLoading, setLogsLoading]   = useState(false);
  const [logTail, setLogTail]           = useState<{ service: string; content: string } | null>(null);
  const [logTailLoading, setLogTailLoading] = useState(false);

  // Backup / Import
  const [backupLoading, setBackupLoading]   = useState(false);
  const [importLoading, setImportLoading]   = useState(false);
  const [importResult, setImportResult]     = useState<ImportResult | null>(null);
  const [activeTab, setActiveTab]           = useState<AdminTabId>('general');
  const importFileRef = useRef<HTMLInputElement>(null);

  // CSV utilisateurs
  const [showCsvImport, setShowCsvImport]       = useState(false);
  const [csvPassword, setCsvPassword]           = useState('');
  const [csvImportLoading, setCsvImportLoading] = useState(false);
  const [csvImportResult, setCsvImportResult]   = useState<CsvImportResult | null>(null);
  const [csvImportError, setCsvImportError]     = useState('');
  const csvFileRef = useRef<HTMLInputElement>(null);

  const loadData = async () => {
    try {
      const [u, c] = await Promise.all([adminService.getAllUsers(), adminService.getConfig()]);
      setUsers(u);
      setConfig(c);
      setNewMax(String(c.maxDivers));
      setNewSiteName(c.siteName ?? '');
      setNewDefaultSlotHours(String(c.defaultSlotHours ?? 2));
      setNewMaxRecurringMonths(String(c.maxRecurringMonths ?? 4));
      setSlotTypesText((c.slotTypes ?? []).join('\n'));
      setExclusiveSlotTypes(c.exclusiveSlotTypes ?? []);
      setClubsText((c.clubs ?? []).join('\n'));
      setLevelsText((c.levels ?? []).join('\n'));
      setBookingOpenHour(c.bookingOpenHour ?? -1);
      setBookingCloseHour(c.bookingCloseHour ?? -1);
      setNotificationEmail(c.notificationBookingEmail ?? '');
      setNotifRegistration(c.notifRegistrationEnabled ?? true);
      setNotifApproved(c.notifApprovedEnabled ?? true);
      setNotifCancelled(c.notifCancelledEnabled ?? true);
      setNotifMovedToWl(c.notifMovedToWlEnabled ?? true);
      setNotifDpNewReg(c.notifDpNewRegEnabled ?? true);
      setNotifSafetyReminder(c.notifSafetyReminderEnabled ?? false);
      setSafetyReminderDelayDays(c.safetyReminderDelayDays ?? 3);
      setSafetyReminderEmailBody(c.safetyReminderEmailBody ?? '');
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

  const handleUpdateDefaultSlotHours = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setLoading(true);
    try {
      const updated = await adminService.updateDefaultSlotHours(Number(newDefaultSlotHours));
      setConfig(updated);
      setNewDefaultSlotHours(String(updated.defaultSlotHours));
      setMsg(`Durée par défaut mise à jour : ${updated.defaultSlotHours}h`);
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    } finally { setLoading(false); }
  };

  const handleUpdateMaxRecurringMonths = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setRecurringLoading(true);
    try {
      const updated = await adminService.updateMaxRecurringMonths(Number(newMaxRecurringMonths));
      setConfig(updated);
      setNewMaxRecurringMonths(String(updated.maxRecurringMonths));
      setMsg(`Durée max de récurrence mise à jour : ${updated.maxRecurringMonths} mois`);
    } catch (err: unknown) { setError(getErrorMessage(err)); }
    finally { setRecurringLoading(false); }
  };

  /** Basculer un rôle dans la liste d'un utilisateur */
  const handleRoleToggle = async (user: User, role: UserRole) => {
    const current = user.roles ?? [user.role];
    const next = current.includes(role)
      ? current.filter(r => r !== role)
      : [...current, role];
    if (next.length === 0) { setError('Un utilisateur doit avoir au moins un rôle'); return; }
    if (role === 'ADMIN' && current.includes('ADMIN') && adminCount <= 1) {
      setError('Impossible de retirer le rôle administrateur du dernier administrateur');
      return;
    }
    try {
      const updated = await adminService.updateRoles(user.id, next);
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      setMsg(`Rôles de ${user.firstName} ${user.lastName} mis à jour`);
    } catch (err: unknown) {
      setError(getErrorMessage(err));
    }
  };

  const handleDeleteUser = async (userId: number, name: string) => {
    const target = users.find(u => u.id === userId);
    if (target && (target.roles ?? [target.role]).includes('ADMIN') && adminCount <= 1) {
      setError('Impossible de supprimer le dernier administrateur');
      return;
    }
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
      setMsg(`Utilisateur "${created.firstName} ${created.lastName}" créé avec succès`);
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

  const handleUpdateExclusiveSlotTypes = async () => {
    setMsg(''); setError(''); setExclusiveLoading(true);
    try {
      const updated = await adminService.updateExclusiveSlotTypes(exclusiveSlotTypes);
      setConfig(updated);
      setExclusiveSlotTypes(updated.exclusiveSlotTypes ?? []);
      setMsg('Types de créneaux exclusifs mis à jour');
    } catch (err: unknown) { setError(getErrorMessage(err)); }
    finally { setExclusiveLoading(false); }
  };

  const handleToggleExclusive = (type: string) => {
    setExclusiveSlotTypes(prev =>
      prev.includes(type) ? prev.filter(t => t !== type) : [...prev, type]
    );
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

  const handleUpdateLevels = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setListLoading(true);
    const items = levelsText.split('\n').map(s => s.trim()).filter(Boolean);
    try {
      const updated = await adminService.updateLevels(items);
      setConfig(updated);
      setLevelsText((updated.levels ?? []).join('\n'));
      setMsg('Niveaux mis à jour');
    } catch (err: unknown) { setError(getErrorMessage(err)); }
    finally { setListLoading(false); }
  };

  const handleUpdateBookingHours = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setBookingHoursLoading(true);
    try {
      const updated = await adminService.updateBookingHours(bookingOpenHour, bookingCloseHour);
      setConfig(updated);
      setBookingOpenHour(updated.bookingOpenHour ?? -1);
      setBookingCloseHour(updated.bookingCloseHour ?? -1);
      const openLabel  = bookingOpenHour  === -1 ? 'illimitée' : `à partir de ${String(bookingOpenHour).padStart(2,'0')}h00`;
      const closeLabel = bookingCloseHour === -1 ? 'illimitée' : `jusqu\'à ${String(bookingCloseHour).padStart(2,'0')}h00`;
      setMsg(`Fenêtre de réservation mise à jour : ${openLabel}, ${closeLabel}`);
    } catch (err: unknown) { setError(getErrorMessage(err)); }
    finally { setBookingHoursLoading(false); }
  };

  const handleUpdateNotificationEmail = async (e: React.FormEvent) => {
    e.preventDefault(); setMsg(''); setError(''); setNotificationEmailLoading(true);
    try {
      const updated = await adminService.updateNotificationEmail(notificationEmail);
      setConfig(updated);
      setNotificationEmail(updated.notificationBookingEmail ?? '');
      setMsg(notificationEmail ? `Email de notification mis à jour : ${notificationEmail}` : 'Notifications désactivées');
    } catch (err: unknown) { setError(getErrorMessage(err)); }
    finally { setNotificationEmailLoading(false); }
  };

  const handleUpdateNotifSettings = async () => {
    setMsg(''); setError(''); setNotifSettingsLoading(true);
    try {
      const updated = await adminService.updateNotifSettings({
        notifRegistrationEnabled: notifRegistration,
        notifApprovedEnabled: notifApproved,
        notifCancelledEnabled: notifCancelled,
        notifMovedToWlEnabled: notifMovedToWl,
        notifDpNewRegEnabled: notifDpNewReg,
        notifSafetyReminderEnabled: notifSafetyReminder,
        safetyReminderDelayDays: safetyReminderDelayDays,
        safetyReminderEmailBody: safetyReminderEmailBody,
      });
      setConfig(updated);
      setMsg('Paramètres de notifications enregistrés.');
    } catch (err: unknown) { setError(getErrorMessage(err)); }
    finally { setNotifSettingsLoading(false); }
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
    setEditForm({ email: user.email, firstName: user.firstName, lastName: user.lastName, phone: user.phone ?? '', licenseNumber: user.licenseNumber ?? '', club: user.club ?? '' });
    setEditError('');
  };

  const filteredUsers = users.filter(u => {
    const q = userSearch.toLowerCase();
    return !q || u.firstName.toLowerCase().includes(q) || u.lastName.toLowerCase().includes(q)
                || u.email.toLowerCase().includes(q) || (u.phone ?? '').toLowerCase().includes(q);
  });
  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / USER_PAGE_SIZE));
  const pagedUsers = filteredUsers.slice((userPage - 1) * USER_PAGE_SIZE, userPage * USER_PAGE_SIZE);

  const cancelEditUser = () => {
    setEditingUserId(null);
    setEditForm({ email: '', firstName: '', lastName: '', phone: '', licenseNumber: '', club: '' });
    setEditError('');
  };

  const handleUpdateUser = async (e: React.FormEvent) => {
    e.preventDefault(); setEditError(''); setEditLoading(true);
    try {
      const updated = await adminService.updateUser(editingUserId!, editForm);
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      setMsg(`Utilisateur "${updated.firstName} ${updated.lastName}" mis à jour`);
      cancelEditUser();
    } catch (err: unknown) {
      setEditError(getErrorMessage(err));
    } finally { setEditLoading(false); }
  };

  const handleLoadLogs = async () => {
    setLogsLoading(true);
    try {
      const data = await adminService.getLogs();
      setLogs(data);
    } catch { setError('Erreur lors du chargement des logs'); }
    finally { setLogsLoading(false); }
  };

  const handleDownloadLog = async (service: string) => {
    try {
      await adminService.downloadLog(service);
    } catch { setError('Erreur lors du téléchargement du log'); }
  };

  const handleTailLog = async (service: string) => {
    setLogTailLoading(true); setLogTail(null);
    try {
      const content = await adminService.tailLog(service, 300);
      setLogTail({ service, content });
    } catch { setError('Erreur lors de la lecture du log'); }
    finally { setLogTailLoading(false); }
  };

  const handleImportFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!confirm(`⚠️ ATTENTION : cet import va EFFACER TOUTES les données actuelles (utilisateurs, créneaux, configuration) et les remplacer par le contenu du fichier "${file.name}".\n\nCette opération est irréversible. Continuer ?`)) {
      e.target.value = '';
      return;
    }
    setImportLoading(true); setImportResult(null); setMsg(''); setError('');
    try {
      const text = await file.text();
      const data = JSON.parse(text);
      const result = await adminService.importBackup(data);
      setImportResult(result);
      if (result.success) {
        setMsg(result.message);
        await loadData(); // Recharger la config importée
      } else {
        setError(result.message);
      }
    } catch (err: unknown) {
      setError('Erreur lors de l\'import : ' + getErrorMessage(err));
    } finally {
      setImportLoading(false);
      if (importFileRef.current) importFileRef.current.value = '';
    }
  };

  const handleExportUsersCsv = async () => {
    try {
      await adminService.exportUsersCsv();
    } catch (err: unknown) {
      setError('Erreur lors de l\'export CSV : ' + getErrorMessage(err));
    }
  };

  const handleImportUsersCsv = async (e: React.FormEvent) => {
    e.preventDefault();
    const file = csvFileRef.current?.files?.[0];
    if (!file) return;
    setCsvImportLoading(true); setCsvImportResult(null); setCsvImportError('');
    try {
      const content = await file.text();
      const result = await adminService.importUsersCsv(content, csvPassword);
      setCsvImportResult(result);
      await loadData();
    } catch (err: unknown) {
      setCsvImportError('Erreur lors de l\'import CSV : ' + getErrorMessage(err));
    } finally {
      setCsvImportLoading(false);
      if (csvFileRef.current) csvFileRef.current.value = '';
      setCsvPassword('');
    }
  };

  return (
    <div className="page">
      <h1>⚙️ Administration</h1>
      {msg   && <div className="alert alert-success">{msg}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <div className="admin-tabs" role="tablist" aria-label="Sections d'administration">
        {ADMIN_TABS.map(tab => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            className={`admin-tab ${activeTab === tab.id ? 'admin-tab-active' : ''}`}
            aria-selected={activeTab === tab.id}
            aria-controls={`admin-panel-${tab.id}`}
            id={`admin-tab-${tab.id}`}
            onClick={() => setActiveTab(tab.id)}
            title={tab.description}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'general' && (
        <div id="admin-panel-general" role="tabpanel" aria-labelledby="admin-tab-general">

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
            <div className="config-item"><span>Durée par défaut</span><strong>{config.defaultSlotHours}h</strong></div>
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
          <form onSubmit={handleUpdateDefaultSlotHours} className="form form-inline" style={{ flex: 1, minWidth: 260 }}>
            <div className="form-group">
              <label>Durée par défaut d'un créneau</label>
              <select value={newDefaultSlotHours} onChange={e => setNewDefaultSlotHours(e.target.value)}>
                {Array.from({ length: 12 }, (_, i) => i + 1).map(h => (
                  <option key={h} value={h}>{h}h</option>
                ))}
              </select>
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>{loading ? '...' : 'Mettre à jour'}</button>
          </form>
        </div>
      </div>

      {/* Accès et inscriptions */}
      <div className="admin-section">
        <h2>🔒 Accès &amp; inscriptions</h2>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <label className="toggle-setting">
            <div className="toggle-setting-info">
              <strong>Accès public au calendrier</strong>
              <span>Si activé, les visiteurs non connectés peuvent consulter les créneaux</span>
            </div>
            <button
              type="button"
              className={`toggle-btn ${config?.publicAccess ? 'toggle-on' : 'toggle-off'}`}
              onClick={async () => {
                if (!config) return;
                const updated = await adminService.updatePublicAccess(!config.publicAccess);
                setConfig(updated);
                setMsg(`Accès public ${updated.publicAccess ? 'activé' : 'désactivé'}`);
              }}
            >
              {config?.publicAccess ? '✅ Activé' : '🔴 Désactivé'}
            </button>
          </label>
          <label className="toggle-setting">
            <div className="toggle-setting-info">
              <strong>Inscription libre</strong>
              <span>Si activé, n'importe qui peut créer un compte depuis la page de connexion</span>
            </div>
            <button
              type="button"
              className={`toggle-btn ${config?.selfRegistration ? 'toggle-on' : 'toggle-off'}`}
              onClick={async () => {
                if (!config) return;
                const updated = await adminService.updateSelfRegistration(!config.selfRegistration);
                setConfig(updated);
                setMsg(`Inscription libre ${updated.selfRegistration ? 'activée' : 'désactivée'}`);
              }}
            >
              {config?.selfRegistration ? '✅ Activée' : '🔴 Désactivée'}
            </button>
          </label>

          {/* Fenêtre horaire de réservation */}
          <div className="toggle-setting" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: 12 }}>
            <div className="toggle-setting-info">
              <strong>🕐 Plage horaire des créneaux</strong>
              <span>
                Interdit la création de créneaux dont l'heure de début se situe en dehors de la plage définie.
                Les inscriptions des plongeurs ne sont pas concernées.
                Mettre <strong>-1</strong> pour désactiver la restriction (pas de limite).
              </span>
            </div>
            {config && (
              <div style={{ fontSize: 13, color: '#6b7280' }}>
                Actuellement&nbsp;:&nbsp;
                {config.bookingOpenHour === -1 && config.bookingCloseHour === -1
                  ? <span style={{ color: '#16a34a' }}>aucune restriction horaire</span>
                  : <>
                    {config.bookingOpenHour !== -1
                      ? <span>ouvert à partir de <strong>{String(config.bookingOpenHour).padStart(2,'0')}h00</strong></span>
                      : <span>ouverture illimitée</span>}
                    {' · '}
                    {config.bookingCloseHour !== -1
                      ? <span>fermé à partir de <strong>{String(config.bookingCloseHour).padStart(2,'0')}h00</strong></span>
                      : <span>fermeture illimitée</span>}
                  </>
                }
              </div>
            )}
            <form onSubmit={handleUpdateBookingHours} className="form form-inline" style={{ gap: 16, flexWrap: 'wrap' }}>
              <div className="form-group" style={{ minWidth: 160 }}>
                <label>Ouverture</label>
                <select
                  value={bookingOpenHour}
                  onChange={e => setBookingOpenHour(Number(e.target.value))}
                >
                  <option value={-1}>— Pas de limite —</option>
                  {Array.from({ length: 24 }, (_, h) => {
                    const forbidden = bookingCloseHour !== -1 && h >= bookingCloseHour;
                    return (
                      <option key={h} value={h} disabled={forbidden}
                        style={forbidden ? { color: '#9ca3af' } : {}}>
                        {String(h).padStart(2, '0')}h00
                      </option>
                    );
                  })}
                </select>
              </div>
              <div className="form-group" style={{ minWidth: 160 }}>
                <label>Fermeture</label>
                <select
                  value={bookingCloseHour}
                  onChange={e => setBookingCloseHour(Number(e.target.value))}
                >
                  <option value={-1}>— Pas de limite —</option>
                  {Array.from({ length: 24 }, (_, h) => {
                    const forbidden = bookingOpenHour !== -1 && h <= bookingOpenHour;
                    return (
                      <option key={h} value={h} disabled={forbidden}
                        style={forbidden ? { color: '#9ca3af' } : {}}>
                        {String(h).padStart(2, '0')}h00
                      </option>
                    );
                  })}
                </select>
              </div>
              <button type="submit" className="btn btn-primary" disabled={bookingHoursLoading} style={{ alignSelf: 'flex-end' }}>
                {bookingHoursLoading ? '...' : '💾 Enregistrer'}
              </button>
            </form>
          </div>

          {/* Email de notification de réservation */}
          <div className="toggle-setting" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: 12 }}>
            <div className="toggle-setting-info">
              <strong>📧 Emails de notification de création de créneau</strong>
              <span>
                Chaque fois qu'un créneau est créé, un email est envoyé à ces adresses.
                Plusieurs adresses possibles, séparées par des virgules. Laisser vide pour désactiver.
              </span>
            </div>
            {config && (
              <div style={{ fontSize: 13, color: '#6b7280' }}>
                Actuellement&nbsp;:&nbsp;
                {config.notificationBookingEmail
                  ? <span style={{ color: '#16a34a' }}>📬 {config.notificationBookingEmail}</span>
                  : <span style={{ color: '#9ca3af' }}>désactivé</span>}
              </div>
            )}
            <form onSubmit={handleUpdateNotificationEmail} className="form form-inline" style={{ gap: 12 }}>
              <div className="form-group" style={{ minWidth: 380 }}>
                <label>Adresses email (séparées par des virgules)</label>
                <input
                  type="text"
                  value={notificationEmail}
                  onChange={e => setNotificationEmail(e.target.value)}
                  placeholder="Ex : directeur@club.fr, secretaire@club.fr"
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={notificationEmailLoading} style={{ alignSelf: 'flex-end' }}>
                {notificationEmailLoading ? '...' : '💾 Enregistrer'}
              </button>
              {notificationEmail && (
                <button type="button" className="btn btn-outline" style={{ alignSelf: 'flex-end' }}
                  onClick={() => { setNotificationEmail(''); }}>
                  ✕ Vider
                </button>
              )}
            </form>
          </div>
        </div>
      </div>

      {/* ── Notifications par e-mail ── */}
      <div className="admin-section">
        <h2>🔔 Notifications par e-mail</h2>
        <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
          Activez ou désactivez les notifications par e-mail selon leur type.
          Lorsqu'une notification est désactivée, son contenu est tracé dans les logs.
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 20 }}>
          {([
            { label: '📩 Confirmation d\'inscription en liste d\'attente (→ plongeur)', value: notifRegistration, setter: setNotifRegistration },
            { label: '✅ Inscription validée (→ plongeur, délai 15 min)', value: notifApproved, setter: setNotifApproved },
            { label: '❌ Inscription annulée/supprimée (→ plongeur)', value: notifCancelled, setter: setNotifCancelled },
            { label: '⏳ Remis en liste d\'attente (→ plongeur, délai 15 min)', value: notifMovedToWl, setter: setNotifMovedToWl },
            { label: '📋 Nouvelles inscriptions sur un créneau (→ directeur de plongée / créateur)', value: notifDpNewReg, setter: setNotifDpNewReg },
            { label: '📋 Rappel fiche de sécurité après la sortie (→ directeur de plongée)', value: notifSafetyReminder, setter: setNotifSafetyReminder },
          ] as { label: string; value: boolean; setter: (v: boolean) => void }[]).map(({ label, value, setter }) => (
            <label key={label} style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
              <input type="checkbox" checked={value} onChange={e => setter(e.target.checked)} />
              <span>{label}</span>
              <span className={`badge ${value ? 'badge-success' : 'badge-muted'}`}>{value ? 'Activé' : 'Désactivé'}</span>
            </label>
          ))}
        </div>
        {/* Configuration du rappel fiche de sécurité */}
        {notifSafetyReminder && (
          <div style={{ background: '#fffbeb', border: '1px solid #fcd34d', borderRadius: 8, padding: 16, marginBottom: 20 }}>
            <h4 style={{ margin: '0 0 12px', color: '#92400e', fontSize: 14 }}>⚙️ Configuration du rappel fiche de sécurité</h4>
            <div className="form-group" style={{ marginBottom: 12 }}>
              <label style={{ fontSize: 13 }}>Délai après la sortie (en jours)</label>
              <input
                type="number"
                min={1}
                max={30}
                value={safetyReminderDelayDays}
                onChange={e => setSafetyReminderDelayDays(Number(e.target.value))}
                style={{ width: 80 }}
              />
            </div>
            <div className="form-group">
              <label style={{ fontSize: 13 }}>
                Contenu du mail
                <span style={{ fontWeight: 400, color: '#6b7280', marginLeft: 6 }}>
                  (variables disponibles&nbsp;: <code>{'{siteName}'}</code>, <code>{'{slotDate}'}</code>, <code>{'{slotLabel}'}</code>)
                </span>
              </label>
              <textarea
                value={safetyReminderEmailBody}
                onChange={e => setSafetyReminderEmailBody(e.target.value)}
                rows={5}
                style={{ width: '100%', fontFamily: 'monospace', fontSize: 13, resize: 'vertical' }}
              />
            </div>
          </div>
        )}
        <button className="btn btn-primary" onClick={handleUpdateNotifSettings} disabled={notifSettingsLoading}>
          {notifSettingsLoading ? '...' : '💾 Enregistrer les paramètres'}
        </button>
      </div>

      {/* ── Créneaux récurrents ── */}
      <div className="admin-section">
        <h2>🔁 Créneaux récurrents</h2>
        {config && (
          <div className="config-item" style={{ marginBottom: 16 }}>
            <span>Durée max actuelle</span>
            <strong>{config.maxRecurringMonths ?? 4} mois</strong>
          </div>
        )}
        <form onSubmit={handleUpdateMaxRecurringMonths} className="form form-inline" style={{ maxWidth: 360 }}>
          <div className="form-group">
            <label>Durée maximale de récurrence (en mois)</label>
            <select value={newMaxRecurringMonths} onChange={e => setNewMaxRecurringMonths(e.target.value)}>
              {[1, 2, 3, 4, 6, 8, 12, 18, 24].map(m => (
                <option key={m} value={m}>{m} mois</option>
              ))}
            </select>
          </div>
          <button type="submit" className="btn btn-primary" disabled={recurringLoading}>
            {recurringLoading ? '...' : '💾 Enregistrer'}
          </button>
        </form>
      </div>

      </div>
      )}

      {activeTab === 'operations' && (
        <div id="admin-panel-operations" role="tabpanel" aria-labelledby="admin-tab-operations">

      {/* ── Téléchargement des logs ── */}
      <div className="admin-section">
        <h2>📋 Logs des services</h2>
        <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
          Téléchargez ou consultez les journaux de l'application, du serveur web et du serveur SMTP.
          En production, les logs Nginx doivent être montés dans le volume partagé
          (<code>/deployments/data/logs/</code>) pour être accessibles ici.
        </p>
        {logs.length === 0 ? (
          <button className="btn btn-outline" onClick={handleLoadLogs} disabled={logsLoading}>
            {logsLoading ? '⏳ Chargement...' : '🔍 Charger la liste des logs'}
          </button>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {logs.map(log => (
              <div key={log.id} style={{
                display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap',
                padding: '12px 16px', borderRadius: 8, border: '1px solid #e5e7eb',
                background: log.available ? '#f9fafb' : '#fff7ed',
              }}>
                <div style={{ flex: 1, minWidth: 200 }}>
                  <strong>{log.label}</strong>
                  <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>
                    {log.available
                      ? `✅ Disponible — ${(log.sizeBytes / 1024).toFixed(1)} Ko`
                      : `⚠️ ${log.info}`}
                  </div>
                </div>
                {log.available && (
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button className="btn btn-small" style={{ background: '#dbeafe', color: '#1d4ed8', border: 'none' }}
                      onClick={() => handleTailLog(log.id)} disabled={logTailLoading}>
                      👁 Aperçu
                    </button>
                    <button className="btn btn-small" style={{ background: '#d1fae5', color: '#065f46', border: 'none' }}
                      onClick={() => handleDownloadLog(log.id)}>
                      ⬇️ Télécharger
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
        {/* Aperçu inline du log */}
        {logTail && (
          <div style={{ marginTop: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <strong style={{ fontSize: 14 }}>📄 Aperçu — {logTail.service} (300 dernières lignes)</strong>
              <button className="btn btn-small btn-outline" onClick={() => setLogTail(null)}>✕ Fermer</button>
            </div>
            <pre style={{
              background: '#111827', color: '#d1fae5', padding: 14, borderRadius: 8,
              fontSize: 11, maxHeight: 400, overflowY: 'auto', whiteSpace: 'pre-wrap',
              wordBreak: 'break-all', fontFamily: 'monospace',
            }}>
              {logTail.content || '(fichier vide)'}
            </pre>
          </div>
        )}
      </div>

      {/* ── Sauvegarde & Restauration ── */}
      <div className="admin-section">
        <h2>💾 Sauvegarde &amp; Restauration</h2>
        <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 20 }}>
          Exportez les données de l'application en JSON pour les sauvegarder ou les migrer vers une autre instance.
          L'import efface toutes les données existantes avant de les restaurer.
        </p>

        {/* Exports */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16, marginBottom: 24 }}>
          <div style={{ flex: 1, minWidth: 260, padding: 16, borderRadius: 8, border: '1px solid #e5e7eb', background: '#f9fafb' }}>
            <h3 style={{ margin: '0 0 6px', fontSize: 15 }}>📤 Export Configuration &amp; Utilisateurs</h3>
            <p style={{ color: '#6b7280', fontSize: 13, margin: '0 0 12px' }}>
              Exporte la configuration du site et la liste des utilisateurs (sans les créneaux).
              Utile pour sauvegarder les paramètres.
            </p>
            <button className="btn btn-primary" disabled={backupLoading}
              onClick={async () => {
                setBackupLoading(true);
                try { await adminService.downloadBackupConfigUsers(); }
                catch { setError('Erreur lors de l\'export'); }
                finally { setBackupLoading(false); }
              }}>
              {backupLoading ? '⏳ ...' : '📥 Télécharger (Config + Utilisateurs)'}
            </button>
          </div>
          <div style={{ flex: 1, minWidth: 260, padding: 16, borderRadius: 8, border: '1px solid #e5e7eb', background: '#f9fafb' }}>
            <h3 style={{ margin: '0 0 6px', fontSize: 15 }}>📤 Export Complet</h3>
            <p style={{ color: '#6b7280', fontSize: 13, margin: '0 0 12px' }}>
              Exporte toutes les données : configuration, utilisateurs, créneaux et plongeurs inscrits.
              Utile pour migrer ou faire une sauvegarde complète.
            </p>
            <button className="btn btn-primary" disabled={backupLoading}
              onClick={async () => {
                setBackupLoading(true);
                try { await adminService.downloadBackupFull(); }
                catch { setError('Erreur lors de l\'export'); }
                finally { setBackupLoading(false); }
              }}>
              {backupLoading ? '⏳ ...' : '📥 Télécharger (Export complet)'}
            </button>
          </div>
        </div>

        {/* Import */}
        <div style={{ padding: 16, borderRadius: 8, border: '2px dashed #fca5a5', background: '#fff7f7' }}>
          <h3 style={{ margin: '0 0 6px', fontSize: 15, color: '#dc2626' }}>
            ⚠️ Import (remplace toutes les données)
          </h3>
          <p style={{ color: '#7f1d1d', fontSize: 13, margin: '0 0 12px' }}>
            <strong>Attention :</strong> l'import supprime <strong>irrémédiablement</strong> toutes les données actuelles
            (configuration, utilisateurs, créneaux, plongeurs) avant de restaurer le fichier sélectionné.
            Assurez-vous d'avoir une sauvegarde récente.
          </p>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <input
              ref={importFileRef}
              type="file"
              accept=".json,application/json"
              onChange={handleImportFile}
              disabled={importLoading}
              style={{ fontSize: 13 }}
            />
            {importLoading && <span style={{ color: '#dc2626' }}>⏳ Import en cours...</span>}
          </div>
          {importResult && (
            <div style={{
              marginTop: 12, padding: 10, borderRadius: 6,
              background: importResult.success ? '#d1fae5' : '#fee2e2',
              color: importResult.success ? '#065f46' : '#991b1b',
              fontSize: 13,
            }}>
              {importResult.message}
              {importResult.success && (
                <span style={{ marginLeft: 8, opacity: 0.8 }}>
                  ({importResult.configRestored} config, {importResult.usersRestored} utilisateurs,
                  {importResult.slotsRestored} créneaux, {importResult.diversRestored} plongeurs,
                  {importResult.palanqueesRestored} palanquées, {importResult.waitingListRestored} liste d'attente)
                </span>
              )}
            </div>
          )}
        </div>
      </div>

      </div>
      )}

      {activeTab === 'catalog' && (
        <div id="admin-panel-catalog" role="tabpanel" aria-labelledby="admin-tab-catalog">

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
          <form onSubmit={handleUpdateLevels} style={{ flex: 1, minWidth: 280 }}>
            <div className="form-group">
              <label style={{ fontWeight: 700 }}>Niveaux de plongeurs</label>
              <textarea rows={8} value={levelsText}
                onChange={e => setLevelsText(e.target.value)}
                placeholder={"Inconnu\nE1\nNiveau 1\n..."}
                style={{ fontFamily: 'monospace', fontSize: 13 }} />
            </div>
            <button type="submit" className="btn btn-primary" disabled={listLoading}>
              {listLoading ? '...' : '💾 Enregistrer les niveaux'}
            </button>
          </form>
        </div>
      </div>

      {/* Créneaux exclusifs */}
      <div className="admin-section">
        <h2>🚫 Créneaux exclusifs (sans chevauchement)</h2>
        <p style={{ color: '#6b7280', fontSize: 13, marginBottom: 16 }}>
          Les types cochés ne peuvent pas se chevaucher dans le calendrier. La création d'un créneau de ce type sera refusée si un autre créneau du même type existe déjà sur la même plage horaire.
        </p>
        {(config?.slotTypes ?? []).length === 0 ? (
          <p style={{ color: '#9ca3af', fontSize: 13 }}>Aucun type de créneau configuré. Ajoutez d'abord des types dans la section « Listes configurables ».</p>
        ) : (
          <>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
              {(config?.slotTypes ?? []).map(type => {
                const isExclusive = exclusiveSlotTypes.includes(type);
                return (
                  <label
                    key={type}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer',
                      padding: '8px 14px', borderRadius: 8, fontSize: 14, userSelect: 'none',
                      border: `2px solid ${isExclusive ? '#dc2626' : '#d1d5db'}`,
                      background: isExclusive ? '#fef2f2' : '#f9fafb',
                      color: isExclusive ? '#dc2626' : '#374151',
                      fontWeight: isExclusive ? 700 : 400,
                      transition: 'all .15s',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={isExclusive}
                      onChange={() => handleToggleExclusive(type)}
                      style={{ accentColor: '#dc2626', width: 16, height: 16 }}
                    />
                    {isExclusive ? '🚫' : '✔️'} {type}
                  </label>
                );
              })}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
              <button
                type="button"
                className="btn btn-primary"
                disabled={exclusiveLoading}
                onClick={handleUpdateExclusiveSlotTypes}
              >
                {exclusiveLoading ? '...' : '💾 Enregistrer'}
              </button>
              {exclusiveSlotTypes.length > 0 && (
                <span style={{ fontSize: 13, color: '#6b7280' }}>
                  {exclusiveSlotTypes.length} type{exclusiveSlotTypes.length > 1 ? 's' : ''} exclusif{exclusiveSlotTypes.length > 1 ? 's' : ''}
                </span>
              )}
            </div>
          </>
        )}
      </div>

      </div>
      )}

      {activeTab === 'users' && (
        <div id="admin-panel-users" role="tabpanel" aria-labelledby="admin-tab-users">

      {/* Gestion des utilisateurs */}
      <div className="admin-section">        <div className="admin-section-header">
          <h2>👥 Gestion des utilisateurs ({users.length})</h2>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <button className="btn btn-secondary" onClick={handleExportUsersCsv} type="button">
              📥 Exporter CSV
            </button>
            <button className="btn btn-secondary" type="button"
              onClick={() => { setShowCsvImport(v => !v); setCsvImportResult(null); setCsvImportError(''); }}>
              {showCsvImport ? '✕ Annuler import' : '📤 Importer CSV'}
            </button>
            <button className="btn btn-primary"
              onClick={() => { setShowCreateForm(v => !v); setCreateError(''); }}>
              {showCreateForm ? '✕ Annuler' : '+ Nouvel utilisateur'}
            </button>
          </div>
        </div>

        {/* Panneau d'import CSV */}
        {showCsvImport && (
          <form onSubmit={handleImportUsersCsv} className="create-user-form">
            <h3>Importer des utilisateurs via CSV</h3>
            <p className="form-hint">
              Format attendu : <code>club;nom;prenom;email;telephone;licence</code> (séparateur point-virgule, encodage UTF-8).
              Les utilisateurs dont l'e-mail existe déjà seront ignorés.
            </p>
            {csvImportError && <div className="alert alert-error">{csvImportError}</div>}
            {csvImportResult && (
              <div className={`alert ${csvImportResult.errors > 0 ? 'alert-warning' : 'alert-success'}`}>
                <strong>Import terminé</strong> — {csvImportResult.imported} importé(s), {csvImportResult.skipped} ignoré(s), {csvImportResult.errors} erreur(s).
                {csvImportResult.messages.length > 0 && (
                  <ul style={{ marginTop: '0.5rem', paddingLeft: '1.2rem' }}>
                    {csvImportResult.messages.map((m, i) => <li key={i}>{m}</li>)}
                  </ul>
                )}
              </div>
            )}
            <div className="form-row">
              <div className="form-group">
                <label>Fichier CSV *</label>
                <input type="file" accept=".csv,text/csv" ref={csvFileRef} required />
              </div>
              <div className="form-group">
                <label>Mot de passe pour tous les comptes importés *</label>
                <input type="password" value={csvPassword} minLength={6} required
                  onChange={e => setCsvPassword(e.target.value)}
                  placeholder="Minimum 6 caractères" />
              </div>
            </div>
            <button type="submit" className="btn btn-primary" disabled={csvImportLoading}>
              {csvImportLoading ? 'Import en cours…' : 'Importer'}
            </button>
          </form>
        )}

        {/* Barre de recherche */}
        <div className="users-search-bar">
          <input
            type="search"
            placeholder="🔍 Rechercher par nom, email ou téléphone..."
            value={userSearch}
            onChange={e => { setUserSearch(e.target.value); setUserPage(1); }}
          />
          {userSearch && (
            <span className="users-search-count">{filteredUsers.length} résultat{filteredUsers.length !== 1 ? 's' : ''}</span>
          )}
        </div>

        {/* Formulaire création */}
        {showCreateForm && (
          <form onSubmit={handleCreateUser} className="create-user-form">
            <h3>Créer un utilisateur</h3>
            {createError && <div className="alert alert-error">{createError}</div>}
            <div className="form-row">
              <div className="form-group">
                <label>Prénom *</label>
                <input placeholder="Jean" value={createForm.firstName}
                  onChange={e => setCreateForm(f => ({ ...f, firstName: e.target.value }))} required minLength={2} />
              </div>
              <div className="form-group">
                <label>Nom *</label>
                <input placeholder="Dupont" value={createForm.lastName}
                  onChange={e => setCreateForm(f => ({ ...f, lastName: e.target.value }))} required minLength={2} />
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
                <input type="tel" placeholder="0612345678 ou +33612345678" value={createForm.phone ?? ''}
                  onChange={e => setCreateForm(f => ({ ...f, phone: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>N° de licence fédérale</label>
                <input type="text" placeholder="Ex : A-14-1223422222" value={createForm.licenseNumber ?? ''}
                  onChange={e => setCreateForm(f => ({ ...f, licenseNumber: e.target.value }))} maxLength={20} />
              </div>
              <div className="form-group">
                <label>Mot de passe *</label>
                <input type="password" placeholder="Min. 6 caractères" value={createForm.password}
                  onChange={e => setCreateForm(f => ({ ...f, password: e.target.value }))}
                  required minLength={6} />
              </div>
            </div>
            <div className="form-group">
              <label>Club d'appartenance</label>
              <select value={createForm.club ?? ''} onChange={e => setCreateForm(f => ({ ...f, club: e.target.value }))}>
                <option value="">— Aucun / Non affilié —</option>
                {(config?.clubs ?? []).map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
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
                <label>Prénom *</label>
                <input placeholder="Jean" value={editForm.firstName}
                  onChange={e => setEditForm(f => ({ ...f, firstName: e.target.value }))} required minLength={2} />
              </div>
              <div className="form-group">
                <label>Nom *</label>
                <input placeholder="Dupont" value={editForm.lastName}
                  onChange={e => setEditForm(f => ({ ...f, lastName: e.target.value }))} required minLength={2} />
              </div>
              <div className="form-group">
                <label>Email *</label>
                <input type="email" placeholder="jean@example.com" value={editForm.email}
                  onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} required />
              </div>
            </div>
            <div className="form-group">
              <label>Téléphone</label>
              <input type="tel" placeholder="0612345678 ou +33612345678" value={editForm.phone ?? ''}
                onChange={e => setEditForm(f => ({ ...f, phone: e.target.value }))} />
            </div>
            <div className="form-group">
              <label>N° de licence fédérale</label>
              <input type="text" placeholder="Ex : A-14-1223422222" value={editForm.licenseNumber ?? ''}
                onChange={e => setEditForm(f => ({ ...f, licenseNumber: e.target.value }))} maxLength={20} />
            </div>
            <div className="form-group">
              <label>Club d'appartenance</label>
              <select value={editForm.club ?? ''} onChange={e => setEditForm(f => ({ ...f, club: e.target.value }))}>
                <option value="">— Aucun / Non affilié —</option>
                {(config?.clubs ?? []).map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
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
              {pagedUsers.map(u => {
                const userRoles = u.roles ?? [u.role];
                const isLastAdmin = userRoles.includes('ADMIN') && adminCount <= 1;
                return (
                  <tr key={u.id}>
                    <td><strong>{u.firstName} {u.lastName}</strong></td>
                    <td>{u.email}</td>
                    <td>{u.phone || <span style={{ color: '#9ca3af' }}>—</span>}</td>
                    <td>
                      <div className="roles-checkboxes-inline">
                        {ALL_ROLES.map(r => {
                          const disableAdminRemoval = r.value === 'ADMIN' && userRoles.includes('ADMIN') && adminCount <= 1;
                          return (
                            <label key={r.value} className="role-checkbox-label small"
                              title={disableAdminRemoval
                                ? 'Dernier administrateur — impossible de retirer ce rôle'
                                : `${userRoles.includes(r.value) ? 'Retirer' : 'Ajouter'} le rôle ${r.label}`}>
                              <input type="checkbox"
                                checked={userRoles.includes(r.value)}
                                onChange={() => handleRoleToggle(u, r.value)}
                                disabled={disableAdminRemoval} />
                              <span>{r.label}</span>
                            </label>
                          );
                        })}
                      </div>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-small"
                          style={{ color: '#2563eb', background: '#dbeafe', border: 'none' }}
                          onClick={() => startEditUser(u)}
                          title="Modifier">✏️ Modifier</button>
                        <button className="btn btn-small"
                          style={isLastAdmin
                            ? { color: '#9ca3af', background: '#f3f4f6', border: 'none', cursor: 'not-allowed' }
                            : { color: '#dc2626', background: '#fee2e2', border: 'none' }}
                          onClick={() => handleDeleteUser(u.id, u.name)}
                          disabled={isLastAdmin}
                          title={isLastAdmin ? 'Dernier administrateur — impossible de supprimer' : 'Supprimer'}>
                          🗑 Supprimer
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="users-pagination">
            <button className="btn btn-small btn-outline" onClick={() => setUserPage(p => p - 1)} disabled={userPage === 1}>‹ Préc.</button>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
              <button key={p} className={`btn btn-small ${p === userPage ? 'btn-primary' : 'btn-outline'}`} onClick={() => setUserPage(p)}>{p}</button>
            ))}
            <button className="btn btn-small btn-outline" onClick={() => setUserPage(p => p + 1)} disabled={userPage === totalPages}>Suiv. ›</button>
          </div>
        )}
      </div>

      </div>
      )}
    </div>
  );
}

