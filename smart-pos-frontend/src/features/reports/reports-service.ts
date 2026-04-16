import { PAYMENT_METHOD } from "@/types/enums";

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

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function getDailyReport(_date: string): Promise<DailyReport> {
  void _date;
  await sleep(250);
  return {
    gross: 5230000,
    discount: 230000,
    net: 5000000,
    transactionCount: 42,
    paymentBreakdown: [
      { paymentMethod: PAYMENT_METHOD.CASH, total: 2500000 },
      { paymentMethod: PAYMENT_METHOD.TRANSFER, total: 1600000 },
      { paymentMethod: PAYMENT_METHOD.EWALLET, total: 900000 },
    ],
  };
}

export async function getTopProductsReport(_from: string, _to: string): Promise<TopProductsReport> {
  void _from;
  void _to;
  await sleep(250);
  return {
    items: [
      { productId: "p-1", sku: "SKU-1001", name: "Arabica Gayo 250g", qtySold: 52, revenue: 4940000 },
      { productId: "p-5", sku: "SKU-1005", name: "Cold Brew Bottle", qtySold: 33, revenue: 2574000 },
      { productId: "p-4", sku: "SKU-1004", name: "Paper Filter V60", qtySold: 28, revenue: 1260000 },
    ],
  };
}
