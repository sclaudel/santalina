import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { User, UserRole, LoginResponse } from '../types';
import { authService } from '../services/authService';

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, firstName: string, lastName: string, phone: string, gdprAccepted: boolean, captchaId: string, captchaAnswer: string) => Promise<string>;
  activateAccount: (token: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  hasRole: (role: UserRole) => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser  = localStorage.getItem('user');
    if (storedToken && storedUser) {
      const parsed = JSON.parse(storedUser);
      // Compat ascendante : ancien format sans firstName/lastName
      if (parsed.name && !parsed.firstName) {
        const parts = (parsed.name as string).trim().split(' ');
        parsed.firstName = parts[0] ?? parsed.name;
        parsed.lastName  = parts.slice(1).join(' ') || '';
      }
      setToken(storedToken);
      setUser(parsed);
    }
  }, []);

  const buildUser = (data: LoginResponse): User => ({
    id: data.userId, email: data.email,
    firstName: data.firstName, lastName: data.lastName,
    name: `${data.firstName} ${data.lastName}`.trim(),
    role: data.role,
    roles: data.roles && data.roles.length > 0 ? data.roles : [data.role],
  });

  const login = async (email: string, password: string) => {
    const data = await authService.login(email, password);
    localStorage.setItem('token', data.token);
    const userData = buildUser(data);
    localStorage.setItem('user', JSON.stringify(userData));
    setToken(data.token);
    setUser(userData);
  };

  const register = async (email: string, firstName: string, lastName: string, phone: string, gdprAccepted: boolean, captchaId: string, captchaAnswer: string): Promise<string> => {
    const data = await authService.register(email, firstName, lastName, phone, gdprAccepted, captchaId, captchaAnswer);
    return data.message;
  };

  const activateAccount = async (token: string, password: string) => {
    const data = await authService.activateAccount(token, password);
    localStorage.setItem('token', data.token);
    const userData = buildUser(data);
    localStorage.setItem('user', JSON.stringify(userData));
    setToken(data.token);
    setUser(userData);
  };

  const logout = () => {
    authService.logout();
    setToken(null);
    setUser(null);
  };

  /** Vérifie si l'utilisateur a le rôle demandé (supporte multi-rôles) */
  const hasRole = (role: UserRole) =>
    !!(user?.roles?.includes(role) || user?.role === role);

  return (
    <AuthContext.Provider value={{
      user, token, login, register, activateAccount, logout,
      isAuthenticated: !!token, hasRole,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
