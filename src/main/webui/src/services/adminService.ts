import api from './api';
import type { User, AppConfig } from '../types';
import type { UserRole } from '../types';

export const adminService = {
  async getAllUsers(): Promise<User[]> {
    const res = await api.get<User[]>('/users');
    return res.data;
  },

  async updateRole(userId: number, role: UserRole): Promise<User> {
    const res = await api.put<User>(`/users/${userId}/role`, { role });
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
};

