import { api } from "./client";
import type { User, UserCreate, UserUpdate } from "../types/api";

export const usersApi = {
  list: () => api.get<User[]>("/users"),

  get: (id: string) => api.get<User>(`/users/${id}`),

  create: (data: UserCreate) =>
    api.post<void>("/users", {
      firstName: data.firstName,
      lastName: data.lastName,
      email: data.email,
      password: data.password,
      role: data.role,
    }),

  update: (id: string, data: UserUpdate) =>
    api.put<void>(`/users/${id}`, {
      firstName: data.firstName,
      lastName: data.lastName,
      email: data.email,
      role: data.role,
      active: data.active,
    }),
};
