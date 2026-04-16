// Types derived directly from backend DTOs
// Source of truth: backend adapters/in/api/dto/

export type Role = "ADMIN" | "CLUB_REPRESENTATIVE";

export type BookingStatus = "APPROVED" | "CANCELLED" | "COMPLETED";
export type BookingSeriesStatus = "ACTIVE" | "CANCELLED";
export type BookingRequestStatus = "PENDING" | "APPROVED" | "REJECTED";
export type BookingSeriesRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

// Java DayOfWeek enum names as serialized by Jackson
export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

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
  bookingSeriesId: string | null; // only set for BOOKING entries that belong to a series
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

// --- Booking Series Requests ---

export interface BookingSeriesRequest {
  id: string;
  title: string;
  description: string;
  weekday: DayOfWeek;
  startTime: string; // "HH:mm:ss"
  endTime: string;
  startDate: string; // "YYYY-MM-DD"
  endDate: string;
  hallId: string;
  hallName: string;
  requestedByName: string;
  status: BookingSeriesRequestStatus;
  rejectionReason: string | null;
}

export interface BookingSeriesRequestCreate {
  title: string;
  description: string;
  weekday: DayOfWeek;
  startTime: string; // "HH:mm:ss"
  endTime: string;
  startDate: string;
  endDate: string;
  hallId: string;
}

export interface BookingSeriesApproveResult {
  createdBookingIds: string[];
  skippedOccurrences: string[]; // ISO LocalDate strings
}

// --- Blocked Times (admin only) ---

export interface BlockedTime {
  id: string;
  reason: string | null; // optional in backend
  startDateTime: string; // ISO LocalDateTime
  endDateTime: string;
  hallId: string;
  hallName: string;
}

export interface BlockedTimeCreate {
  hallId: string;
  reason?: string;
  startDateTime: string; // ISO LocalDateTime "YYYY-MM-DDTHH:mm:ss"
  endDateTime: string;
}

// --- Statistics (admin only) ---

export interface SeriesUsage {
  bookingSeriesId: string;
  title: string;
  bookingCount: number;
}

export interface HallStatistics {
  hallId: string;
  hallName: string;
  totalBookings: number;
  cancelledBookings: number;
  totalParticipants: number;
  utilizationPercent: number;
  topSeries: SeriesUsage[];
}

export interface SeriesStatistics {
  bookingSeriesId: string;
  title: string;
  hallName: string;
  totalAppointments: number;
  conductedAppointments: number;
  cancelledAppointments: number;
  totalParticipants: number;
  averageParticipants: number;
}

export interface SeriesOccurrence {
  bookingId: string;
  startDateTime: string;
  endDateTime: string;
  cancelled: boolean;
  conducted: boolean;
  participantCount: number | null;
  feedbackComment: string | null;
}

export interface SeriesStatisticsDetail extends SeriesStatistics {
  responsibleUserName: string;
  occurrences: SeriesOccurrence[];
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

// Matches CreateUserDTO on the backend
export interface UserCreate {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: Role;
}

// Matches PUT /users/{id} — fullName and id fields are ignored by backend
export interface UserUpdate {
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  active: boolean;
}

// --- Booking Series ---

// Matches BookingSeriesDTO on the backend
export interface BookingSeries {
  id: string;
  title: string;
  description: string;
  weekday: DayOfWeek;
  startTime: string; // "HH:mm:ss"
  endTime: string;
  startDate: string; // "YYYY-MM-DD"
  endDate: string;
  hallId: string;
  hallName: string;
  status: BookingSeriesStatus;
  responsibleUserName: string;
}

// --- Booking Update (admin only) ---

// Matches PUT /bookings/{id} which accepts BookingRequestDTO fields
export interface BookingUpdate {
  hallId: string;
  title: string;
  description: string;
  startDateTime: string;
  endDateTime: string;
}
