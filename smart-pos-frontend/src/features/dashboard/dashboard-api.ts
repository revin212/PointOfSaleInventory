import type { DashboardData } from "@/features/dashboard/dashboard-types";
import { getSales } from "@/features/sales/sales-api";
import { listOnHand } from "@/features/inventory/inventory-api";
import { getDailyReport, getTopProductsReport } from "@/features/reports/reports-api";
import { ROLE, type Role } from "@/types/enums";

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

function daysAgoIsoDate(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

export async function getDashboardData(role: Role): Promise<DashboardData> {
  const today = todayIsoDate();
  const weekAgo = daysAgoIsoDate(6);

  const [dailyResult, topProductsResult, lowStockResult, recentSalesResult] = await Promise.allSettled([
    role === ROLE.WAREHOUSE
      ? Promise.resolve(null)
      : getDailyReport(today),
    role === ROLE.WAREHOUSE
      ? Promise.resolve(null)
      : getTopProductsReport(weekAgo, today),
    listOnHand({ lowOnly: true, page: 0, size: 10 }),
    role === ROLE.WAREHOUSE
      ? Promise.resolve(null)
      : getSales({ page: 0, size: 5 }),
  ]);

  const daily = dailyResult.status === "fulfilled" ? dailyResult.value : null;
  const topProducts = topProductsResult.status === "fulfilled" ? topProductsResult.value : null;
  const lowStock = lowStockResult.status === "fulfilled" ? lowStockResult.value : null;
  const recentSales = recentSalesResult.status === "fulfilled" ? recentSalesResult.value : null;

  return {
    kpis: {
      todaySales: daily?.net ?? 0,
      transactionCount: daily?.transactionCount ?? 0,
      lowStockCount: lowStock?.totalElements ?? 0,
    },
    recentSales:
      recentSales?.content.map((sale) => ({
        id: sale.id,
        invoiceNo: sale.invoiceNo,
        cashierName: sale.cashierName,
        paymentMethod: sale.paymentMethod,
        total: sale.totals.total,
        createdAt: sale.createdAt,
      })) ?? [],
    topProducts:
      topProducts?.items.map((item) => ({
        productId: item.productId,
        name: item.name,
        sku: item.sku,
        qtySold: item.qtySold,
        revenue: item.revenue,
      })) ?? [],
    lowStockItems:
      lowStock?.content.map((row) => ({
        productId: row.productId,
        name: row.name,
        sku: row.sku,
        stock: row.stock,
        threshold: row.lowStockThreshold,
      })) ?? [],
  };
}
