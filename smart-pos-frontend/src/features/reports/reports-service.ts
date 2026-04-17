import { USE_MOCKS } from "@/lib/env";
import * as apiImpl from "@/features/reports/reports-api";
import * as mockImpl from "@/features/reports/reports-service.mock";

export type { DailyReport, TopProductsReport } from "@/features/reports/reports-types";

const impl = USE_MOCKS ? mockImpl : apiImpl;

export const getDailyReport = impl.getDailyReport;
export const getTopProductsReport = impl.getTopProductsReport;
