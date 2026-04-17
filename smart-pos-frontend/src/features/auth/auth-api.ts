import { api } from "@/lib/api-client";
import { tokenStore } from "@/lib/token-store";
import type { AuthUser } from "@/types/auth";
import { ROLE, type Role } from "@/types/enums";

type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: {
    id: string;
    name: string;
    email: string;
    role: Role;
  };
};

type MeResponse = {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
};

function normalizeRole(role: string): Role {
  if (role === ROLE.OWNER || role === ROLE.CASHIER || role === ROLE.WAREHOUSE) {
    return role;
  }
  return ROLE.CASHIER;
}

export async function login(email: string, password: string): Promise<AuthUser> {
  const response = await api.post<LoginResponse>(
    "/auth/login",
    { email, password },
    { auth: false, skipRefresh: true },
  );
  tokenStore.setTokens(response.accessToken, response.refreshToken);
  return {
    id: response.user.id,
    name: response.user.name,
    email: response.user.email,
    role: normalizeRole(response.user.role),
    active: true,
  };
}

export async function me(): Promise<AuthUser> {
  const response = await api.get<MeResponse>("/auth/me");
  return {
    id: response.id,
    name: response.name,
    email: response.email,
    role: normalizeRole(response.role),
    active: response.active,
  };
}

export async function logout(): Promise<void> {
  const refreshToken = tokenStore.getRefreshToken();
  try {
    if (refreshToken) {
      await api.post("/auth/logout", { refreshToken }, { auth: false, skipRefresh: true });
    }
  } catch {
    /* ignore logout errors; we still clear locally */
  } finally {
    tokenStore.clear();
  }
}
