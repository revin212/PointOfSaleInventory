import { USE_MOCKS } from "@/lib/env";
import * as apiImpl from "@/features/users/users-api";
import * as mockImpl from "@/features/users/users-service.mock";

export type { UserRecord } from "@/features/users/users-types";

const impl = USE_MOCKS ? mockImpl : apiImpl;

export const listUsers = impl.listUsers;
export const createUser = impl.createUser;
export const updateUser = impl.updateUser;
export const setUserActive = impl.setUserActive;
