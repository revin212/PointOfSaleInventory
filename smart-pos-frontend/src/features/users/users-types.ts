import type { Role } from "@/types/enums";

export type UserRecord = {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
};
