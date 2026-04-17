import { MOCK_SALES, type SaleRecord } from "@/mocks/commerce";
import { PAYMENT_METHOD, type PaymentMethod } from "@/types/enums";

type SalesQuery = {
  query?: string;
  paymentMethod?: PaymentMethod | "ALL";
  page?: number;
  size?: number;
};

function sleep(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export async function getSales(query: SalesQuery): Promise<{
  content: SaleRecord[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}> {
  await sleep(400);
  const page = query.page ?? 0;
  const size = query.size ?? 10;

  let filtered = MOCK_SALES;

  if (query.query) {
    const q = query.query.toLowerCase();
    filtered = filtered.filter(
      (sale) =>
        sale.invoiceNo.toLowerCase().includes(q) ||
        sale.cashierName.toLowerCase().includes(q) ||
        sale.items.some((item) => item.name.toLowerCase().includes(q) || item.sku.toLowerCase().includes(q)),
    );
  }

  if (query.paymentMethod && query.paymentMethod !== "ALL") {
    filtered = filtered.filter((sale) => sale.paymentMethod === query.paymentMethod);
  }

  const start = page * size;
  const content = filtered.slice(start, start + size);

  return {
    content,
    page,
    size,
    totalElements: filtered.length,
    totalPages: Math.max(1, Math.ceil(filtered.length / size)),
  };
}

export async function getSaleById(id: string): Promise<SaleRecord> {
  await sleep(300);
  const sale = MOCK_SALES.find((entry) => entry.id === id);
  if (!sale) {
    throw new Error("Sale not found.");
  }
  return sale;
}

export async function cancelSaleById(id: string): Promise<{ success: boolean; status: "CANCELLED" }> {
  await sleep(450);
  const sale = MOCK_SALES.find((entry) => entry.id === id);
  if (!sale) {
    throw new Error("Sale not found.");
  }
  sale.status = "CANCELLED";
  return { success: true, status: "CANCELLED" };
}

export async function createSale(payload: {
  items: Array<{ productId: string; qty: number; unitPrice: number; lineDiscount?: number }>;
  discount: number;
  paymentMethod: PaymentMethod;
  paidAmount: number;
}): Promise<{ id: string; invoiceNo: string; status: "COMPLETED"; totals: { subtotal: number; discount: number; total: number; paidAmount: number; changeAmount: number } }> {
  await sleep(550);
  const subtotal = payload.items.reduce((sum, item) => sum + item.unitPrice * item.qty, 0);
  const lineDiscount = payload.items.reduce((sum, item) => sum + (item.lineDiscount ?? 0), 0);
  const total = Math.max(0, subtotal - lineDiscount - payload.discount);
  const changeAmount = Math.max(0, payload.paidAmount - total);
  return {
    id: `s-new-${Date.now()}`,
    invoiceNo: `INV-NEW-${Date.now()}`,
    status: "COMPLETED",
    totals: {
      subtotal,
      discount: lineDiscount + payload.discount,
      total,
      paidAmount: payload.paidAmount,
      changeAmount,
    },
  };
}

export const paymentMethodOptions = [
  { label: "All", value: "ALL" as const },
  { label: "Cash", value: PAYMENT_METHOD.CASH },
  { label: "Transfer", value: PAYMENT_METHOD.TRANSFER },
  { label: "E-Wallet", value: PAYMENT_METHOD.EWALLET },
];
