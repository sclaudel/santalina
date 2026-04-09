import api from './api';
import type { LoginResponse, User } from '../types';

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const res = await api.post<LoginResponse>('/auth/login', { email, password });
    return res.data;
  },

  async getCaptcha(): Promise<{ id: string; image: string }> {
    const res = await api.get<{ id: string; image: string }>('/auth/captcha');
    return res.data;
  },

  async register(email: string, firstName: string, lastName: string, phone: string, consentGiven: boolean, captchaId: string, captchaAnswer: string): Promise<{ message: string }> {
    const res = await api.post<{ message: string }>('/auth/register', { email, firstName, lastName, phone, consentGiven, captchaId, captchaAnswer });
    return res.data;
  },

  async activateAccount(token: string, password: string): Promise<LoginResponse> {
    const res = await api.post<LoginResponse>('/auth/activate', { token, password });
    return res.data;
  },

  async requestPasswordReset(email: string): Promise<void> {
    await api.post('/auth/password-reset/request', { email });
  },

  async confirmPasswordReset(token: string, newPassword: string): Promise<void> {
    await api.post('/auth/password-reset/confirm', { token, newPassword });
  },

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await api.post('/auth/change-password', { currentPassword, newPassword });
  },

  async getProfile(): Promise<User> {
    const res = await api.get<User>('/users/me');
    return res.data;
  },

  async updateProfile(email: string, firstName: string, lastName: string, phone?: string, licenseNumber?: string): Promise<User> {
    const res = await api.put<User>('/users/me', { email, firstName, lastName, phone: phone || null, licenseNumber: licenseNumber || null });
    return res.data;
  },

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getStoredUser(): User | null {
    const raw = localStorage.getItem('user');
    return raw ? JSON.parse(raw) : null;
  },

  getToken(): string | null {
    return localStorage.getItem('token');
  },
};

