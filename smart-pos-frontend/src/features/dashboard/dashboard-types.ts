export type DashboardData = {
  kpis: {
    todaySales: number;
    transactionCount: number;
    lowStockCount: number;
  };
  recentSales: Array<{
    id: string;
    invoiceNo: string;
    cashierName: string;
    paymentMethod: string;
    total: number;
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
