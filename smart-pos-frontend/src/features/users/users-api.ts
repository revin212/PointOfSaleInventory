import { api, type PageResponse } from "@/lib/api-client";
import type { UserRecord } from "@/features/users/users-types";
import type { Role } from "@/types/enums";

type BackendUser = {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
};

function mapUser(raw: BackendUser): UserRecord {
  return {
    id: raw.id,
    name: raw.name,
    email: raw.email,
    role: raw.role,
    active: raw.active,
  };
}

export async function listUsers(): Promise<UserRecord[]> {
  const response = await api.get<PageResponse<BackendUser>>("/users", { query: { size: 200 } });
  return response.content.map(mapUser);
}

export async function createUser(payload: Omit<UserRecord, "id"> & { password?: string }): Promise<UserRecord> {
  const raw = await api.post<BackendUser>("/users", {
    name: payload.name,
    email: payload.email,
    password: payload.password && payload.password.length > 0 ? payload.password : "Password123!",
    role: payload.role,
    active: payload.active,
  });
  return mapUser(raw);
}

export async function updateUser(id: string, payload: Omit<UserRecord, "id"> & { password?: string }): Promise<UserRecord> {
  const raw = await api.put<BackendUser>(`/users/${id}`, {
    name: payload.name,
    email: payload.email,
    role: payload.role,
    password: payload.password && payload.password.length > 0 ? payload.password : undefined,
  });
  if (raw.active !== payload.active) {
    const updated = await api.patch<BackendUser>(`/users/${id}/active`, { active: payload.active });
    return mapUser(updated);
  }
  return mapUser(raw);
}

export async function setUserActive(id: string, active: boolean): Promise<{ success: boolean }> {
  await api.patch<unknown>(`/users/${id}/active`, { active });
  return { success: true };
}
