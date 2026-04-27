import type { DashboardData } from "@/features/dashboard/dashboard-types";
import { getSaleById, getSales } from "@/features/sales/sales-api";
import { listMovements, listOnHand } from "@/features/inventory/inventory-api";
import { getDailyReport, getTopProductsReport } from "@/features/reports/reports-api";
import { ROLE, type Role } from "@/types/enums";

function todayIsoDate(): string {
  const now = new Date();
  const offsetMs = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - offsetMs).toISOString().slice(0, 10);
}

function daysAgoIsoDate(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  const offsetMs = d.getTimezoneOffset() * 60_000;
  return new Date(d.getTime() - offsetMs).toISOString().slice(0, 10);
}

function computeTopProductsFromSalesDetails(
  saleDetails: Array<Awaited<ReturnType<typeof getSaleById>>>,
  limit: number,
): DashboardData["topProducts"] {
  const byProduct = new Map<
    string,
    { productId: string; sku: string; name: string; qtySold: number; revenue: number }
  >();

  for (const sale of saleDetails) {
    for (const item of sale.items) {
      const key = item.productId;
      const existing = byProduct.get(key) ?? {
        productId: item.productId,
        sku: item.sku,
        name: item.name,
        qtySold: 0,
        revenue: 0,
      };
      const lineDiscount = Number(item.lineDiscount ?? 0);
      const gross = Number(item.unitPrice ?? 0) * Number(item.qty ?? 0);
      const lineTotal = Math.max(0, gross - lineDiscount);
      existing.qtySold += Number(item.qty ?? 0);
      existing.revenue += lineTotal;
      byProduct.set(key, existing);
    }
  }

  return Array.from(byProduct.values())
    .sort((a, b) => b.revenue - a.revenue || b.qtySold - a.qtySold)
    .slice(0, limit);
}

export async function getDashboardData(role: Role): Promise<DashboardData> {
  const today = todayIsoDate();
  const weekAgo = daysAgoIsoDate(6);

  const [dailyResult, topProductsResult, lowStockResult, onHandAllResult, recentSalesResult, recentMovementsResult] =
    await Promise.allSettled([
      role === ROLE.OWNER ? getDailyReport(today) : Promise.resolve(null),
      role === ROLE.OWNER ? getTopProductsReport(weekAgo, today) : Promise.resolve(null),
      listOnHand({ lowOnly: true, page: 0, size: 10 }),
      // used to compute skuCount + outOfStockCount (warehouse needs this most)
      listOnHand({ lowOnly: false, page: 0, size: 200 }),
      role === ROLE.WAREHOUSE ? Promise.resolve(null) : getSales({ from: today, to: today, page: 0, size: 10 }),
      role === ROLE.WAREHOUSE ? listMovements({ page: 0, size: 8 }) : Promise.resolve(null),
    ]);

  const daily = dailyResult.status === "fulfilled" ? dailyResult.value : null;
  const topProducts = topProductsResult.status === "fulfilled" ? topProductsResult.value : null;
  const lowStock = lowStockResult.status === "fulfilled" ? lowStockResult.value : null;
  const onHandAll = onHandAllResult.status === "fulfilled" ? onHandAllResult.value : null;
  const recentSales = recentSalesResult.status === "fulfilled" ? recentSalesResult.value : null;
  const recentMovements = recentMovementsResult.status === "fulfilled" ? recentMovementsResult.value : null;

  const cashierTodaySales =
    role === ROLE.CASHIER
      ? (recentSales?.content ?? []).reduce((sum, sale) => sum + (sale.totals.total ?? 0), 0)
      : 0;
  const cashierTxCount = role === ROLE.CASHIER ? Number(recentSales?.totalElements ?? 0) : 0;

  const skuCount = onHandAll?.totalElements ?? 0;
  const outOfStockCount = (onHandAll?.content ?? []).filter((row) => row.stock <= 0).length;

  let cashierTopProducts: DashboardData["topProducts"] = [];
  if (role === ROLE.CASHIER && (recentSales?.content ?? []).length > 0) {
    // Reports endpoints are OWNER-only, so we compute from sale details.
    const saleIds = recentSales!.content.map((s) => s.id).slice(0, 10);
    const saleDetailsSettled = await Promise.allSettled(saleIds.map((id) => getSaleById(id)));
    const saleDetails = saleDetailsSettled
      .filter((r): r is PromiseFulfilledResult<Awaited<ReturnType<typeof getSaleById>>> => r.status === "fulfilled")
      .map((r) => r.value);
    cashierTopProducts = computeTopProductsFromSalesDetails(saleDetails, 5);
  }

  return {
    kpis: {
      todaySales: role === ROLE.OWNER ? (daily?.net ?? 0) : cashierTodaySales,
      transactionCount: role === ROLE.OWNER ? (daily?.transactionCount ?? 0) : cashierTxCount,
      lowStockCount: lowStock?.totalElements ?? 0,
      outOfStockCount,
      skuCount,
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
    recentMovements:
      recentMovements?.content.map((m) => ({
        id: m.id,
        productId: m.productId,
        productName: m.productName,
        type: m.type,
        qtyDelta: m.qtyDelta,
        reason: m.reason,
        createdAt: m.createdAt,
      })) ?? [],
    topProducts:
      role === ROLE.OWNER
        ? topProducts?.items.map((item) => ({
            productId: item.productId,
            name: item.name,
            sku: item.sku,
            qtySold: item.qtySold,
            revenue: item.revenue,
          })) ?? []
        : cashierTopProducts,
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
