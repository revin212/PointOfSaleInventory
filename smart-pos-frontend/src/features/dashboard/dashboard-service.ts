import { USE_MOCKS } from "@/lib/env";
import * as apiImpl from "@/features/dashboard/dashboard-api";
import * as mockImpl from "@/features/dashboard/dashboard-service.mock";

export type { DashboardData } from "@/features/dashboard/dashboard-types";

const impl = USE_MOCKS ? mockImpl : apiImpl;

export const getDashboardData = impl.getDashboardData;
