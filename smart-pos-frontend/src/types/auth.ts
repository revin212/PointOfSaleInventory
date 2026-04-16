import type { Role } from "@/types/enums";

export type AuthUser = {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
};
