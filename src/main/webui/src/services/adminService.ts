import api from './api';
import type { User, AppConfig, CreateUserRequest } from '../types';
import type { UserRole } from '../types';

export const adminService = {
  async getAllUsers(): Promise<User[]> {
    const res = await api.get<User[]>('/users');
    return res.data;
  },

  async createUser(req: CreateUserRequest): Promise<User> {
    const res = await api.post<User>('/users', req);
    return res.data;
  },

  async deleteUser(userId: number): Promise<void> {
    await api.delete(`/users/${userId}`);
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
};
