import type { DashboardData } from "@/features/dashboard/dashboard-types";
import { PAYMENT_METHOD, ROLE, type Role } from "@/types/enums";

export type { DashboardData };

const ownerData: DashboardData = {
  kpis: {
    todaySales: 12500000,
    transactionCount: 45,
    lowStockCount: 3,
  },
  recentSales: [
    {
      id: "s-101",
      invoiceNo: "INV-2026-0101",
      cashierName: "Cashier User",
      paymentMethod: PAYMENT_METHOD.CASH,
      total: 450000,
      createdAt: "2026-04-16T08:32:00.000Z",
    },
    {
      id: "s-102",
      invoiceNo: "INV-2026-0102",
      cashierName: "Cashier User",
      paymentMethod: PAYMENT_METHOD.EWALLET,
      total: 890000,
      createdAt: "2026-04-16T09:12:00.000Z",
    },
    {
      id: "s-103",
      invoiceNo: "INV-2026-0103",
      cashierName: "Owner User",
      paymentMethod: PAYMENT_METHOD.TRANSFER,
      total: 1250000,
      createdAt: "2026-04-16T09:52:00.000Z",
    },
  ],
  topProducts: [
    { productId: "p-1", name: "Arabica Gayo 250g", sku: "SKU-1001", qtySold: 52, revenue: 4940000 },
    { productId: "p-5", name: "Cold Brew Bottle", sku: "SKU-1005", qtySold: 33, revenue: 2574000 },
    { productId: "p-4", name: "Paper Filter V60", sku: "SKU-1004", qtySold: 28, revenue: 1260000 },
  ],
  lowStockItems: [
    { productId: "p-3", name: "Manual Brew Kettle", sku: "SKU-1003", stock: 6, threshold: 10 },
    { productId: "p-2", name: "Robusta Blend 500g", sku: "SKU-1002", stock: 8, threshold: 10 },
    { productId: "p-7", name: "Barista Milk Jug", sku: "SKU-1020", stock: 2, threshold: 6 },
  ],
};

const cashierData: DashboardData = {
  ...ownerData,
  kpis: {
    todaySales: 8400000,
    transactionCount: 31,
    lowStockCount: 0,
  },
  lowStockItems: [],
};

const warehouseData: DashboardData = {
  ...ownerData,
  kpis: {
    todaySales: 0,
    transactionCount: 0,
    lowStockCount: ownerData.lowStockItems.length,
  },
  recentSales: [],
};

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function getDashboardData(role: Role): Promise<DashboardData> {
  await sleep(350);
  if (role === ROLE.CASHIER) {
    return cashierData;
  }
  if (role === ROLE.WAREHOUSE) {
    return warehouseData;
  }
  return ownerData;
}
