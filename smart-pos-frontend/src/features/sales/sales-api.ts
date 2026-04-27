import { api, type PageResponse } from "@/lib/api-client";
import type { SaleRecord } from "@/mocks/commerce";
import { PAYMENT_METHOD, type PaymentMethod } from "@/types/enums";

type BackendSaleSummary = {
  id: string;
  invoiceNo: string;
  cashierId: string;
  cashierName: string;
  status: "COMPLETED" | "CANCELLED";
  paymentMethod: PaymentMethod;
  subtotal: number;
  discount: number;
  total: number;
  itemCount: number;
  createdAt: string;
};

type BackendSaleItem = {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  qty: number;
  unitPrice: number;
  lineDiscount?: number;
  lineTotal: number;
};

type BackendSaleTotals = {
  subtotal: number;
  discount: number;
  netAmount?: number;
  taxRate?: number;
  taxAmount?: number;
  adminFee?: number;
  total: number;
  paidAmount: number;
  changeAmount: number;
};

type BackendSaleDetail = {
  id: string;
  invoiceNo: string;
  cashierId: string;
  cashierName: string;
  status: "COMPLETED" | "CANCELLED";
  paymentMethod: PaymentMethod;
  totals: BackendSaleTotals;
  createdAt: string;
  cancelledAt?: string | null;
  cancelledBy?: string | null;
  cancelReason?: string | null;
  items: BackendSaleItem[];
};

type BackendSaleCreateResponse = {
  id: string;
  invoiceNo: string;
  status: "COMPLETED" | "CANCELLED";
  totals: BackendSaleTotals;
};

function mapSummary(raw: BackendSaleSummary): SaleRecord {
  return {
    id: raw.id,
    invoiceNo: raw.invoiceNo,
    createdAt: raw.createdAt,
    cashierName: raw.cashierName,
    paymentMethod: raw.paymentMethod,
    status: raw.status,
    items: [],
    totals: {
      subtotal: Number(raw.subtotal ?? 0),
      discount: Number(raw.discount ?? 0),
      netAmount: undefined,
      taxRate: undefined,
      taxAmount: undefined,
      adminFee: undefined,
      total: Number(raw.total ?? 0),
      paidAmount: 0,
      changeAmount: 0,
    },
  };
}

function mapDetail(raw: BackendSaleDetail): SaleRecord {
  return {
    id: raw.id,
    invoiceNo: raw.invoiceNo,
    createdAt: raw.createdAt,
    cashierName: raw.cashierName,
    paymentMethod: raw.paymentMethod,
    status: raw.status,
    items: raw.items.map((item) => ({
      productId: item.productId,
      sku: item.productSku,
      name: item.productName,
      qty: item.qty,
      unitPrice: Number(item.unitPrice),
      lineDiscount: item.lineDiscount ? Number(item.lineDiscount) : undefined,
    })),
    totals: {
      subtotal: Number(raw.totals.subtotal ?? 0),
      discount: Number(raw.totals.discount ?? 0),
      netAmount: raw.totals.netAmount != null ? Number(raw.totals.netAmount) : undefined,
      taxRate: raw.totals.taxRate != null ? Number(raw.totals.taxRate) : undefined,
      taxAmount: raw.totals.taxAmount != null ? Number(raw.totals.taxAmount) : undefined,
      adminFee: raw.totals.adminFee != null ? Number(raw.totals.adminFee) : undefined,
      total: Number(raw.totals.total ?? 0),
      paidAmount: Number(raw.totals.paidAmount ?? 0),
      changeAmount: Number(raw.totals.changeAmount ?? 0),
    },
  };
}

export async function getSales(params: {
  query?: string;
  paymentMethod?: PaymentMethod | "ALL";
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}): Promise<{ content: SaleRecord[]; page: number; size: number; totalElements: number; totalPages: number }> {
  const response = await api.get<PageResponse<BackendSaleSummary>>("/sales", {
    query: {
      from: params.from,
      to: params.to,
      paymentMethod: params.paymentMethod && params.paymentMethod !== "ALL" ? params.paymentMethod : undefined,
      page: params.page,
      size: params.size,
    },
  });
  let content = response.content.map(mapSummary);
  if (params.query) {
    const q = params.query.toLowerCase();
    content = content.filter(
      (sale) =>
        sale.invoiceNo.toLowerCase().includes(q) ||
        sale.cashierName.toLowerCase().includes(q),
    );
  }
  return {
    content,
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
  };
}

export async function getSaleById(id: string): Promise<SaleRecord> {
  const raw = await api.get<BackendSaleDetail>(`/sales/${id}`);
  return mapDetail(raw);
}

export async function cancelSaleById(id: string, reason?: string): Promise<{ success: boolean; status: "CANCELLED" }> {
  const response = await api.post<{ success: boolean; status: "COMPLETED" | "CANCELLED" }>(
    `/sales/${id}/cancel`,
    { reason: reason ?? "Cancelled from UI" },
  );
  return { success: response.success, status: "CANCELLED" };
}

export async function createSale(payload: {
  items: Array<{ productId: string; qty: number; unitPrice: number; lineDiscount?: number }>;
  discount: number;
  paymentMethod: PaymentMethod;
  paymentTypeId?: string;
  paidAmount: number;
}): Promise<BackendSaleCreateResponse> {
  return api.post<BackendSaleCreateResponse>("/sales", {
    items: payload.items.map((item) => ({
      productId: item.productId,
      qty: item.qty,
      unitPrice: item.unitPrice,
      lineDiscount: item.lineDiscount ?? 0,
    })),
    discount: payload.discount,
    paymentMethod: payload.paymentMethod,
    paidAmount: payload.paidAmount,
    paymentTypeId: payload.paymentTypeId,
  });
}

export const paymentMethodOptions = [
  { label: "All", value: "ALL" as const },
  { label: "Cash", value: PAYMENT_METHOD.CASH },
  { label: "Transfer", value: PAYMENT_METHOD.TRANSFER },
  { label: "E-Wallet", value: PAYMENT_METHOD.EWALLET },
];
