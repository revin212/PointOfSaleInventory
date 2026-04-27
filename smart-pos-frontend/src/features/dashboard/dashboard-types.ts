export type DashboardData = {
  kpis: {
    todaySales: number;
    transactionCount: number;
    lowStockCount: number;
    outOfStockCount: number;
    skuCount: number;
  };
  recentSales: Array<{
    id: string;
    invoiceNo: string;
    cashierName: string;
    paymentMethod: string;
    total: number;
    createdAt: string;
  }>;
  recentMovements: Array<{
    id: string;
    productId: string;
    productName: string;
    type: string;
    qtyDelta: number;
    reason: string;
    createdAt: string;
  }>;
  topProducts: Array<{
    productId: string;
    name: string;
    sku: string;
    qtySold: number;
    revenue: number;
  }>;
  lowStockItems: Array<{
    productId: string;
    name: string;
    sku: string;
    stock: number;
    threshold: number;
  }>;
};
