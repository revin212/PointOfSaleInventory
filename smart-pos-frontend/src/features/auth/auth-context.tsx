/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useMemo, useState, type ReactNode } from "react";

import { ROLE, type Role } from "@/types/enums";
import type { AuthUser } from "@/types/auth";

type AuthContextValue = {
  user: AuthUser | null;
  isAuthenticated: boolean;
  loginAs: (role: Role) => void;
  logout: () => void;
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
  const [user, setUser] = useState<AuthUser | null>(roleSeeds[ROLE.OWNER]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      loginAs: (role) => setUser(roleSeeds[role]),
      logout: () => setUser(null),
    }),
    [user],
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
