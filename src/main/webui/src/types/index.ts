// Types partagés de l'application

export type UserRole = 'ADMIN' | 'DIVE_DIRECTOR' | 'DIVER';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  name: string;
  phone?: string;
  licenseNumber?: string;
  role: UserRole;
  roles: UserRole[];
}

export interface LoginResponse {
  token: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  userId: number;
  roles?: UserRole[];
}

export interface CreateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  phone?: string;
  roles: UserRole[];
}

export interface UpdateUserAdminRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface UserSearchResult {
  id: number;
  firstName: string;
  lastName: string;
  name: string;
  email: string;
  phone?: string;
  licenseNumber?: string;
}

export interface SlotDiver {
  id: number;
  firstName: string;
  lastName: string;
  level: string;
  email?: string;
  phone?: string;
  isDirector: boolean;
  aptitudes?: string;
  licenseNumber?: string;
}

export interface SlotDiverRequest {
  firstName: string;
  lastName: string;
  level: string;
  email?: string;
  phone?: string;
  isDirector: boolean;
  aptitudes?: string;
  licenseNumber?: string;
}

export interface DiveSlot {
  id: number;
  slotDate: string;       // YYYY-MM-DD
  startTime: string;      // HH:mm
  endTime: string;        // HH:mm
  diverCount: number;
  title: string | null;
  notes: string | null;
  slotType: string | null;
  club: string | null;
  createdById: number;
  createdByName: string;
  divers: SlotDiver[];
}

export interface SlotRequest {
  slotDate: string;
  startTime: string;
  endTime: string;
  diverCount: number;
  title?: string;
  notes?: string;
  slotType?: string;
  club?: string;
}

export interface AppConfig {
  maxDivers: number;
  slotMinHours: number;
  slotMaxHours: number;
  slotResolutionMinutes: number;
  siteName: string;
  slotTypes: string[];
  clubs: string[];
  levels: string[];
  publicAccess: boolean;
  selfRegistration: boolean;
  bookingOpenHour: number;
  bookingCloseHour: number;
  exclusiveSlotTypes: string[];
  defaultSlotHours: number;
}

export interface PeriodStat {
  label: string;
  slots: number;
  divers: number;
}

export interface GroupStat {
  label: string;
  slots: number;
  divers: number;
}

export interface DpPeriodStat {
  label: string;
  directions: number;
  avgDivers: number;
}

export interface DpStat {
  name: string;
  totalDirections: number;
  avgDiversPerSlot: number;
  byYear: DpPeriodStat[];
  byMonth: DpPeriodStat[];
}

export interface StatsResponse {
  byMonth: PeriodStat[];
  byYear: PeriodStat[];
  byClub: GroupStat[];
  byType: GroupStat[];
  totalSlots: number;
  totalDivers: number;
  totalClubs: number;
  avgDiversPerSlot: number;
  byDayOfWeek: PeriodStat[];
  byLevel: GroupStat[];
  byDiveDirector: DpStat[];
}

export interface MyStatsResponse {
  byMonth: PeriodStat[];
  byYear: PeriodStat[];
  byClub: GroupStat[];
  byType: GroupStat[];
  totalSlots: number;
  totalDivers: number;
  avgDiversPerSlot: number;
  byDayOfWeek: PeriodStat[];
  byLevel: GroupStat[];
}

export interface Palanquee {
  id: number;
  name: string;
  position: number;
  divers: SlotDiver[];
}
