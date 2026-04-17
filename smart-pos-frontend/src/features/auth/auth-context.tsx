/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

import { USE_MOCKS } from "@/lib/env";
import { onAuthEvent, tokenStore } from "@/lib/token-store";
import * as authApi from "@/features/auth/auth-api";
import { ROLE, type Role } from "@/types/enums";
import type { AuthUser } from "@/types/auth";

type AuthContextValue = {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isBootstrapping: boolean;
  login: (email: string, password: string, role?: Role) => Promise<AuthUser>;
  loginAs: (role: Role) => void;
  logout: () => Promise<void>;
};

const roleSeeds: Record<Role, AuthUser> = {
  OWNER: {
    id: "owner-1",
    name: "Owner User",
    email: "owner@store.com",
    role: ROLE.OWNER,
    active: true,
  },
  CASHIER: {
    id: "cashier-1",
    name: "Cashier User",
    email: "cashier@store.com",
    role: ROLE.CASHIER,
    active: true,
  },
  WAREHOUSE: {
    id: "warehouse-1",
    name: "Warehouse User",
    email: "warehouse@store.com",
    role: ROLE.WAREHOUSE,
    active: true,
  },
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(USE_MOCKS ? roleSeeds[ROLE.OWNER] : null);
  const [isBootstrapping, setIsBootstrapping] = useState<boolean>(!USE_MOCKS);

  useEffect(() => {
    if (USE_MOCKS) {
      setIsBootstrapping(false);
      return;
    }
    const refreshToken = tokenStore.getRefreshToken();
    if (!refreshToken) {
      setIsBootstrapping(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const me = await authApi.me();
        if (!cancelled) setUser(me);
      } catch {
        if (!cancelled) {
          tokenStore.clear();
          setUser(null);
        }
      } finally {
        if (!cancelled) setIsBootstrapping(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (USE_MOCKS) return;
    const unsubscribe = onAuthEvent("unauthenticated", () => {
      tokenStore.clear();
      setUser(null);
    });
    return unsubscribe;
  }, []);

  const login = useCallback(async (email: string, password: string, role?: Role): Promise<AuthUser> => {
    if (USE_MOCKS) {
      const next = role ? roleSeeds[role] : roleSeeds[ROLE.OWNER];
      setUser(next);
      return next;
    }
    const next = await authApi.login(email, password);
    setUser(next);
    return next;
  }, []);

  const loginAs = useCallback((role: Role) => {
    if (!USE_MOCKS) return;
    setUser(roleSeeds[role]);
  }, []);

  const logout = useCallback(async () => {
    if (USE_MOCKS) {
      setUser(null);
      return;
    }
    await authApi.logout();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isBootstrapping,
      login,
      loginAs,
      logout,
    }),
    [user, isBootstrapping, login, loginAs, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
