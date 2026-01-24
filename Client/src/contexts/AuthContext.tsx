import { createContext, useContext, useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { Session } from "@/lib/mock/types";
import { mockApi } from "@/lib/mock/api";

type AuthState = {
  session: Session | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<Session>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const qc = useQueryClient();

  const sessionQuery = useQuery({
    queryKey: ["auth", "session"],
    queryFn: () => mockApi.auth.getSession(),
    staleTime: 30_000,
  });

  const loginMutation = useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) => mockApi.auth.login(email, password),
    onSuccess: (session) => {
      qc.setQueryData(["auth", "session"], session);
    },
  });

  const logoutMutation = useMutation({
    mutationFn: () => mockApi.auth.logout(),
    onSuccess: () => {
      qc.setQueryData(["auth", "session"], null);
      qc.invalidateQueries();
    },
  });

  const value = useMemo<AuthState>(
    () => ({
      session: sessionQuery.data ?? null,
      isLoading: sessionQuery.isLoading,
      login: async (email, password) => loginMutation.mutateAsync({ email, password }),
      logout: async () => {
        await logoutMutation.mutateAsync();
      },
      refresh: async () => {
        await qc.invalidateQueries({ queryKey: ["auth", "session"] });
      },
    }),
    [sessionQuery.data, sessionQuery.isLoading, loginMutation, logoutMutation, qc],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
