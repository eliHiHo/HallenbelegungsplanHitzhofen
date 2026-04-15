import { api } from "./client";
import type {
  LoginRequest,
  LoginResponse,
  CurrentUser,
  ForgotPasswordRequest,
  ResetPasswordRequest,
} from "../types/api";

export const authApi = {
  login: (data: LoginRequest) =>
    api.post<LoginResponse>("/auth/login", data),

  logout: () =>
    api.post<void>("/auth/logout"),

  me: () =>
    api.get<CurrentUser>("/auth/me"),

  forgotPassword: (data: ForgotPasswordRequest) =>
    api.post<void>("/auth/forgot-password", data),

  resetPassword: (data: ResetPasswordRequest) =>
    api.post<void>("/auth/reset-password", data),
};
