import { useQuery } from "@tanstack/react-query";
import { hallsApi } from "../../shared/api/halls";

export function useHalls(includeInactive?: boolean) {
  return useQuery({
    queryKey: ["halls", { includeInactive }],
    queryFn: () => hallsApi.list(includeInactive),
  });
}
