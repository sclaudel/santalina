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
  notifOnRegistration?: boolean;
  notifOnApproved?: boolean;
  notifOnCancelled?: boolean;
  notifOnMovedToWaitlist?: boolean;
  notifOnDpRegistration?: boolean;
  notifOnCreatorRegistration?: boolean;
}

export interface LoginResponse {
  token: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  userId: number;
  roles?: UserRole[];
  phone?: string;
}

export interface CreateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  phone?: string;
  licenseNumber?: string;
  roles: UserRole[];
}

export interface UpdateUserAdminRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  licenseNumber?: string;
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
  userId?: number;
  medicalCertDate?: string;
  comment?: string;
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
  registrationOpen: boolean;
  registrationOpensAt: string | null; // ISO datetime or null
  waitingListCount?: number;
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
  // Récurrence
  recurring?: boolean;
  recurringDays?: number[];   // 1=Lun … 7=Dim (ISO)
  recurringUntil?: string;    // YYYY-MM-DD
}

export interface BatchSlotResponse {
  slots: DiveSlot[];
  created: number;
  skipped: number;
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
  notificationBookingEmail: string;
  maxRecurringMonths: number;
  notifRegistrationEnabled: boolean;
  notifApprovedEnabled: boolean;
  notifCancelledEnabled: boolean;
  notifMovedToWlEnabled: boolean;
  notifDpNewRegEnabled: boolean;
}

// Logs
export interface LogInfo {
  id: string;
  label: string;
  available: boolean;
  sizeBytes: number;
  info: string;
}

// Backup / Import
export interface ImportResult {
  success: boolean;
  message: string;
  configRestored: number;
  usersRestored: number;
  slotsRestored: number;
  diversRestored: number;
  palanqueesRestored: number;
  waitingListRestored: number;
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
  depth?: string;
  duration?: string;
  divers: SlotDiver[];
}

// ── Liste d'attente ──────────────────────────────────────────────────────────

export const DIVER_LEVELS = ['N1','N2','N3','N4','N5','E2','E3','E4','PE12','PE40','PE60','MF1','MF2'] as const;
export const DP_LEVELS = ['N5','E3','E4','MF1','MF2'] as const;

export const PREPARED_LEVELS = [
  'Aucun','N1','N2','N3','N4','N5','MF1','MF2',
  'E1','E2','E3','E4','PE12','PE40','PE60',
  'PA20','PA40','PA60','PN','PNC','PB1','PB2','PV1','PV2'
] as const;

export interface WaitingListEntry {
  id: number;
  slotId: number;
  firstName: string;
  lastName: string;
  email: string;
  level: string;
  numberOfDives?: number;
  lastDiveDate?: string;     // YYYY-MM-DD
  preparedLevel?: string;
  comment?: string;
  registeredAt: string;     // ISO datetime
  medicalCertDate?: string;  // YYYY-MM-DD
  licenseConfirmed?: boolean;
}

export interface WaitingListRequest {
  firstName: string;
  lastName: string;
  email: string;
  emailConfirm: string;
  level: string;
  numberOfDives?: number;
  lastDiveDate?: string;
  preparedLevel?: string;
  comment?: string;
  medicalCertDate: string;   // YYYY-MM-DD
  licenseConfirmed: boolean;
}

export interface UpdateRegistrationRequest {
  registrationOpen: boolean;
  registrationOpensAt?: string | null; // ISO datetime or null
}
