import { api } from "@/lib/api-client";
import type { DailyReport, TopProductsReport } from "@/features/reports/reports-types";
import type { PaymentMethod } from "@/types/enums";

type BackendDailySummary = {
  date: string;
  salesCount: number;
  totalRevenue: number;
  totalItemsSold: number;
  cancelledCount: number;
  cancelledAmount: number;
  byPaymentMethod: Array<{ paymentMethod: PaymentMethod; count: number; total: number }>;
};

type BackendTopProducts = {
  from: string;
  to: string;
  limit: number;
  rows: Array<{ productId: string; sku: string; name: string; qtySold: number; totalRevenue: number }>;
};

export async function getDailyReport(date: string): Promise<DailyReport> {
  const response = await api.get<BackendDailySummary>("/reports/daily-summary", {
    query: { date },
  });
  const net = Number(response.totalRevenue ?? 0);
  return {
    gross: net,
    discount: 0,
    net,
    transactionCount: Number(response.salesCount ?? 0),
    paymentBreakdown: (response.byPaymentMethod ?? []).map((entry) => ({
      paymentMethod: entry.paymentMethod,
      total: Number(entry.total ?? 0),
    })),
  };
}

export async function getTopProductsReport(from: string, to: string): Promise<TopProductsReport> {
  const response = await api.get<BackendTopProducts>("/reports/top-products", {
    query: { from, to },
  });
  return {
    items: (response.rows ?? []).map((row) => ({
      productId: row.productId,
      sku: row.sku,
      name: row.name,
      qtySold: Number(row.qtySold ?? 0),
      revenue: Number(row.totalRevenue ?? 0),
    })),
  };
}
