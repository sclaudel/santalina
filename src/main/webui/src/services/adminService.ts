import api from './api';
import type { User, AppConfig, CreateUserRequest, UpdateUserAdminRequest, UserRole, UserSearchResult, LogInfo, ImportResult, CsvImportResult } from '../types';

export const adminService = {
  async getAllUsers(): Promise<User[]> {
    const res = await api.get<User[]>('/users');
    return res.data;
  },

  async searchUsers(q: string): Promise<UserSearchResult[]> {
    const res = await api.get<UserSearchResult[]>('/users/search', { params: { q } });
    return res.data;
  },

  async getDiveDirectors(): Promise<UserSearchResult[]> {
    const res = await api.get<UserSearchResult[]>('/users/dive-directors');
    return res.data;
  },

  async createUser(req: CreateUserRequest): Promise<User> {
    const res = await api.post<User>('/users', req);
    return res.data;
  },

  async deleteUser(userId: number): Promise<void> {
    await api.delete(`/users/${userId}`);
  },

  async updateUser(userId: number, req: UpdateUserAdminRequest): Promise<User> {
    const res = await api.put<User>(`/users/${userId}`, req);
    return res.data;
  },

  async updateRoles(userId: number, roles: UserRole[]): Promise<User> {
    const res = await api.put<User>(`/users/${userId}/roles`, { roles });
    return res.data;
  },

  async getConfig(): Promise<AppConfig> {
    const res = await api.get<AppConfig>('/config');
    return res.data;
  },

  async updateMaxDivers(maxDivers: number): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/max-divers', { maxDivers });
    return res.data;
  },

  async updateSiteName(siteName: string): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/site-name', { siteName });
    return res.data;
  },

  async updateSlotTypes(items: string[]): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/slot-types', { items });
    return res.data;
  },

  async updateClubs(items: string[]): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/clubs', { items });
    return res.data;
  },

  async updateLevels(items: string[]): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/levels', { items });
    return res.data;
  },

  async updateDiverLevels(items: string[]): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/diver-levels', { items });
    return res.data;
  },

  async updateDpLevels(items: string[]): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/dp-levels', { items });
    return res.data;
  },

  async updatePreparedLevels(items: string[]): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/prepared-levels', { items });
    return res.data;
  },

  async updateAptitudes(items: string[]): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/aptitudes', { items });
    return res.data;
  },

  async updatePublicAccess(value: boolean): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/public-access', { value });
    return res.data;
  },

  async updateSelfRegistration(value: boolean): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/self-registration', { value });
    return res.data;
  },

  async updateBookingHours(bookingOpenHour: number, bookingCloseHour: number): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/booking-hours', { bookingOpenHour, bookingCloseHour });
    return res.data;
  },

  async updateExclusiveSlotTypes(items: string[]): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/exclusive-slot-types', { items });
    return res.data;
  },

  async updateDefaultSlotHours(defaultSlotHours: number): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/default-slot-hours', { defaultSlotHours });
    return res.data;
  },

  async updateSlotMaxHours(slotMaxHours: number): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/slot-max-hours', { slotMaxHours });
    return res.data;
  },

  async updateNotificationEmail(email: string): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/notification-email', { email });
    return res.data;
  },

  async updateMaxRecurringMonths(maxRecurringMonths: number): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/max-recurring-months', { maxRecurringMonths });
    return res.data;
  },

  async updateNotifSettings(settings: {
    notifRegistrationEnabled: boolean;
    notifApprovedEnabled: boolean;
    notifCancelledEnabled: boolean;
    notifMovedToWlEnabled: boolean;
    notifDpNewRegEnabled: boolean;
    notifSafetyReminderEnabled: boolean;
    safetyReminderDelayDays: number;
    safetyReminderEmailBody: string;
    safetyReminderActivationDate?: string;
  }): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/notification-settings', settings);
    return res.data;
  },

  async updateMaintenanceMode(value: boolean): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/maintenance-mode', { value });
    return res.data;
  },

  async updateReportEmailSettings(settings: {
    reportEmailEnabled: boolean;
    reportEmailPeriodDays: number;
    reportEmailRecipients: string;
  }): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/report-email-settings', settings);
    return res.data;
  },

  async updateOrganizerMailTemplate(template: string): Promise<AppConfig> {
    const res = await api.put<AppConfig>('/config/organizer-mail-template', { template });
    return res.data;
  },

  async sendManualReport(from: string, to: string, recipients: string, club?: string): Promise<{ count: number }> {
    const res = await api.post<{ count: number }>('/config/report-email-send', { fromDate: from, toDate: to, recipients, ...(club ? { club } : {}) });
    return res.data;
  },

  async downloadReport(from: string, to: string, club?: string): Promise<void> {
    const res = await api.get('/config/report-email-download', {
      params: { from, to, ...(club ? { club } : {}) },
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv;charset=UTF-8' }));
    const link = document.createElement('a');
    link.href = url;
    const clubSuffix = club ? `_${club}` : '';
    link.setAttribute('download', `inscriptions_${from}_${to}${clubSuffix}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  // ---- Logs ----

  async getLogs(): Promise<LogInfo[]> {
    const res = await api.get<LogInfo[]>('/admin/logs');
    return res.data;
  },

  async downloadLog(service: string): Promise<void> {
    const res = await api.get(`/admin/logs/${service}/download`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    const disposition = res.headers['content-disposition'] ?? '';
    const match = disposition.match(/filename="?([^"]+)"?/);
    link.setAttribute('download', match ? match[1] : `${service}.log`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async tailLog(service: string, lines = 200): Promise<string> {
    const res = await api.get<string>(`/admin/logs/${service}/tail`, {
      params: { lines },
      responseType: 'text',
    });
    return res.data;
  },

  // ---- Backup / Import ----

  async downloadBackupConfigUsers(): Promise<void> {
    const res = await api.get('/admin/backup/export/config-users', { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/json' }));
    const link = document.createElement('a');
    link.href = url;
    const today = new Date().toISOString().slice(0, 10);
    link.setAttribute('download', `santalina-config-users-${today}.json`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async downloadBackupFull(): Promise<void> {
    const res = await api.get('/admin/backup/export/full', { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/json' }));
    const link = document.createElement('a');
    link.href = url;
    const today = new Date().toISOString().slice(0, 10);
    link.setAttribute('download', `santalina-full-${today}.json`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async importBackup(data: unknown): Promise<ImportResult> {
    const res = await api.post<ImportResult>('/admin/backup/import', data);
    return res.data;
  },

  // ---- CSV utilisateurs ----

  async exportUsersCsv(): Promise<void> {
    const res = await api.get('/users/export/csv', { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv;charset=UTF-8' }));
    const link = document.createElement('a');
    link.href = url;
    const today = new Date().toISOString().slice(0, 10);
    link.setAttribute('download', `utilisateurs-${today}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async importUsersCsv(csvContent: string, password: string): Promise<CsvImportResult> {
    const res = await api.post<CsvImportResult>('/users/import/csv', { csvContent, password });
    return res.data;
  },
};
