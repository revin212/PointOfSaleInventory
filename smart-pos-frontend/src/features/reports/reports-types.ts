export type DailyReport = {
  gross: number;
  discount: number;
  net: number;
  transactionCount: number;
  paymentBreakdown: Array<{ paymentMethod: string; total: number }>;
};

export type TopProductsReport = {
  items: Array<{ productId: string; sku: string; name: string; qtySold: number; revenue: number }>;
};
