import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { mockApi } from "@/lib/mock/api";
import { FunctionEntity } from "@/lib/mock/types";

// Keys
export const keys = {
    session: ["session"],
    project: ["project"],
    functions: {
        base: ["functions"],
        list: (q?: string) => ["functions", "list", q],
        detail: (id: string) => ["functions", "detail", id],
    },
    files: {
        base: ["files"],
        list: (q?: string) => ["files", "list", q],
    },
    invocations: {
        base: ["invocations"],
        list: (q?: any) => ["invocations", "list", q],
    },
    logs: {
        base: ["logs"],
        list: (q?: any) => ["logs", "list", q],
    },
};

// --- Auth ---
// --- Auth ---
export function useSession() {
    return useQuery({
        queryKey: keys.session,
        queryFn: () => mockApi.auth.getSession(),
        staleTime: 1000 * 60 * 5, // 5 mins
    });
}

export function useLogin() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ email, password }: { email: string; password: string }) =>
            mockApi.auth.login(email, password),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: keys.session });
        },
    });
}

export function useLogout() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: () => mockApi.auth.logout(),
        onSuccess: () => {
            queryClient.setQueryData(keys.session, null);
            queryClient.invalidateQueries({ queryKey: keys.session });
        },
    });
}

export function useInvocations(query?: { q?: string; status?: "success" | "error" | "all" }) {
    return useQuery({
        queryKey: keys.invocations.list(query),
        queryFn: () => mockApi.invocations.list(query),
    });
}

// --- Functions ---
export function useFunctions(search?: string) {
    return useQuery({
        queryKey: keys.functions.list(search),
        queryFn: () => mockApi.functions.list({ q: search }),
    });
}

export function useFunction(id: string) {
    return useQuery({
        queryKey: keys.functions.detail(id),
        queryFn: () => mockApi.functions.get(id),
        enabled: !!id,
    });
}

export function useCreateFunction() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (data: Omit<FunctionEntity, "id" | "createdAt" | "updatedAt" | "invocations24h" | "errors24h">) =>
            mockApi.functions.create(data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: keys.functions.base });
        },
    });
}

export function useDeleteFunction() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (id: string) => mockApi.functions.remove(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: keys.functions.base });
        },
    });
}

export function useInvokeFunction() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, input }: { id: string; input: string }) =>
            mockApi.functions.invoke(id, input),
        onSuccess: (_, variables) => {
            queryClient.invalidateQueries({ queryKey: keys.logs.base });
            queryClient.invalidateQueries({ queryKey: keys.functions.detail(variables.id) });
        },
    });
}

export function useLogs(query?: { q?: string; level?: "info" | "warn" | "error" | "all" }) {
    return useQuery({
        queryKey: keys.logs.list(query),
        queryFn: () => mockApi.logs.list(query),
    });
}
