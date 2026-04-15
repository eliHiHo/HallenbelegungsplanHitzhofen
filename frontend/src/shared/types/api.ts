// Types derived directly from backend DTOs
// Source of truth: backend adapters/in/api/dto/

export type Role = "ADMIN" | "CLUB_REPRESENTATIVE";

export type BookingStatus = "APPROVED" | "CANCELLED" | "COMPLETED";
export type BookingSeriesStatus = "ACTIVE" | "CANCELLED";
export type BookingRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

// CalendarEntryDTO — type field values from backend (CalendarEntry domain model)
export type CalendarEntryType = "BOOKING" | "BLOCKED_TIME";

// HallType from Hall domain enum
export type HallType = "FULL" | "PART_SMALL" | "PART_LARGE";

// --- Auth ---

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface CurrentUser {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  role: Role;
  active: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

// --- Halls ---

export interface Hall {
  id: string;
  name: string;
  description: string;
  type: HallType;
  active: boolean;
}

// --- Calendar ---

export interface CalendarEntry {
  id: string;
  type: CalendarEntryType;
  title: string;
  description: string;
  startDateTime: string; // ISO LocalDateTime from backend
  endDateTime: string;
  hallId: string;
  hallName: string;
  responsibleUserName: string;
  status: BookingStatus | null; // null for BLOCKED_TIME entries
  ownEntry: boolean;
}

export interface CalendarWeek {
  weekStart: string; // ISO LocalDate
  weekEnd: string;
  entries: CalendarEntry[];
}

export interface CalendarDay {
  day: string; // ISO LocalDate
  entries: CalendarEntry[];
}

// --- Bookings ---

export interface Booking {
  id: string;
  title: string;
  description: string;
  startDateTime: string;
  endDateTime: string;
  hallId: string;
  hallName: string;
  status: BookingStatus;
  responsibleUserName: string;
  participantCount: number | null;
  feedbackComment: string | null;
  canViewFeedback: boolean;
  canEdit: boolean;
  canCancel: boolean;
}

// --- Booking Requests ---

export interface BookingRequest {
  id: string;
  title: string;
  description: string;
  startDateTime: string;
  endDateTime: string;
  hallId: string;
  hallName: string;
  requestedByName: string;
  status: BookingRequestStatus;
  rejectionReason: string | null;
}

export interface BookingRequestCreate {
  title: string;
  description: string;
  startDateTime: string;
  endDateTime: string;
  hallId: string;
}

export interface RejectionRequest {
  reason: string;
}

// --- Users (admin only) ---

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  role: Role;
  active: boolean;
}
