// Types partagés de l'application

export type UserRole = 'ADMIN' | 'DIVE_DIRECTOR' | 'GUEST';

export interface User {
  id: number;
  email: string;
  name: string;
  role: UserRole;
}

export interface LoginResponse {
  token: string;
  email: string;
  name: string;
  role: UserRole;
  userId: number;
}

export interface DiveSlot {
  id: number;
  slotDate: string;       // YYYY-MM-DD
  startTime: string;      // HH:mm
  endTime: string;        // HH:mm
  diverCount: number;
  title: string | null;
  notes: string | null;
  createdById: number;
  createdByName: string;
}

export interface SlotRequest {
  slotDate: string;
  startTime: string;
  endTime: string;
  diverCount: number;
  title?: string;
  notes?: string;
}

export interface AppConfig {
  maxDivers: number;
  slotMinHours: number;
  slotMaxHours: number;
  slotResolutionMinutes: number;
}

