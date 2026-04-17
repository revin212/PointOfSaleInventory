import { ROLE } from "@/types/enums";
import type { UserRecord } from "@/features/users/users-types";

export type { UserRecord };

let users: UserRecord[] = [
  { id: "u-1", name: "Owner User", email: "owner@store.com", role: ROLE.OWNER, active: true },
  { id: "u-2", name: "Cashier User", email: "cashier@store.com", role: ROLE.CASHIER, active: true },
  { id: "u-3", name: "Warehouse User", email: "warehouse@store.com", role: ROLE.WAREHOUSE, active: true },
];

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function listUsers() {
  await sleep(250);
  return [...users];
}

export async function createUser(payload: Omit<UserRecord, "id"> & { password?: string }) {
  await sleep(250);
  const exists = users.some((user) => user.email.toLowerCase() === payload.email.toLowerCase());
  if (exists) {
    throw new Error("Email is already used.");
  }
  const { password: _password, ...rest } = payload;
  void _password;
  const created: UserRecord = { ...rest, id: `u-${Date.now()}` };
  users = [created, ...users];
  return created;
}

export async function updateUser(id: string, payload: Omit<UserRecord, "id"> & { password?: string }) {
  await sleep(250);
  const existing = users.find((user) => user.id === id);
  if (!existing) {
    throw new Error("User not found.");
  }
  const { password: _password, ...rest } = payload;
  void _password;
  const updated: UserRecord = { ...rest, id };
  users = users.map((user) => (user.id === id ? updated : user));
  return updated;
}

export async function setUserActive(id: string, active: boolean) {
  await sleep(250);
  const existing = users.find((user) => user.id === id);
  if (!existing) {
    throw new Error("User not found.");
  }
  users = users.map((user) => (user.id === id ? { ...user, active } : user));
  return { success: true };
}
