import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { usersApi } from "../../shared/api/users";
import type { UserCreate, UserUpdate } from "../../shared/types/api";

export const USERS_KEY = ["users"];

export function useUsers() {
  return useQuery({
    queryKey: USERS_KEY,
    queryFn: usersApi.list,
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UserCreate) => usersApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: USERS_KEY });
    },
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UserUpdate }) =>
      usersApi.update(id, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: USERS_KEY });
    },
  });
}
