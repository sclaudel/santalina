import api from './api';
import type { User, AppConfig, CreateUserRequest, UpdateUserAdminRequest, UserRole, UserSearchResult } from '../types';

export const adminService = {
  async getAllUsers(): Promise<User[]> {
    const res = await api.get<User[]>('/users');
    return res.data;
  },

  async searchUsers(q: string): Promise<UserSearchResult[]> {
    const res = await api.get<UserSearchResult[]>('/users/search', { params: { q } });
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
};
