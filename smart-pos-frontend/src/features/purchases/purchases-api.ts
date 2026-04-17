import { api, type PageResponse } from "@/lib/api-client";
import type { PurchaseItem, PurchaseRecord } from "@/features/purchases/purchases-types";
import type { PurchaseStatus } from "@/types/purchase";

type BackendPurchaseSummary = {
  id: string;
  supplierId: string;
  supplierName: string;
  status: PurchaseStatus;
  itemCount: number;
  totalCost: number;
  createdAt: string;
  updatedAt: string;
};

type BackendPurchaseItem = {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  qtyOrdered: number;
  qtyReceivedTotal: number;
  qtyOutstanding: number;
  cost: number;
};

type BackendPurchaseDetail = {
  id: string;
  supplierId: string;
  supplierName: string;
  status: PurchaseStatus;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  items: BackendPurchaseItem[];
};

function shortNo(id: string): string {
  const compact = id.replace(/-/g, "").slice(0, 8).toUpperCase();
  return `PO-${compact}`;
}

function mapSummary(raw: BackendPurchaseSummary): PurchaseRecord {
  return {
    id: raw.id,
    purchaseNo: shortNo(raw.id),
    supplierId: raw.supplierId,
    supplierName: raw.supplierName,
    status: raw.status,
    createdAt: raw.createdAt,
    items: [],
  };
}

function mapItem(raw: BackendPurchaseItem): PurchaseItem {
  return {
    productId: raw.productId,
    productName: raw.productName,
    qtyOrdered: raw.qtyOrdered,
    qtyReceived: raw.qtyReceivedTotal,
    cost: Number(raw.cost ?? 0),
  };
}

function mapDetail(raw: BackendPurchaseDetail): PurchaseRecord {
  return {
    id: raw.id,
    purchaseNo: shortNo(raw.id),
    supplierId: raw.supplierId,
    supplierName: raw.supplierName,
    status: raw.status,
    createdAt: raw.createdAt,
    items: raw.items.map(mapItem),
  };
}

export async function listPurchases(params: {
  status?: PurchaseStatus | "ALL";
  page?: number;
  size?: number;
}): Promise<{ content: PurchaseRecord[]; page: number; size: number; totalElements: number; totalPages: number }> {
  const response = await api.get<PageResponse<BackendPurchaseSummary>>("/purchases", {
    query: {
      status: params.status && params.status !== "ALL" ? params.status : undefined,
      page: params.page,
      size: params.size,
    },
  });
  return {
    content: response.content.map(mapSummary),
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
  };
}

export async function getPurchaseById(id: string): Promise<PurchaseRecord> {
  const raw = await api.get<BackendPurchaseDetail>(`/purchases/${id}`);
  return mapDetail(raw);
}

export async function createPurchase(payload: {
  supplierId: string;
  items: Array<{ productId: string; qtyOrdered: number; cost: number }>;
}): Promise<PurchaseRecord> {
  const raw = await api.post<BackendPurchaseDetail>("/purchases", {
    supplierId: payload.supplierId,
    items: payload.items.map((item) => ({
      productId: item.productId,
      qtyOrdered: item.qtyOrdered,
      cost: item.cost,
    })),
  });
  return mapDetail(raw);
}

export async function receivePurchase(payload: {
  purchaseId: string;
  items: Array<{ productId: string; qtyReceived: number; cost: number }>;
}): Promise<{ success: boolean; status: PurchaseStatus }> {
  const items = payload.items.filter((item) => item.qtyReceived > 0);
  if (items.length === 0) {
    throw new Error("At least one line must have qty > 0 to receive.");
  }
  return api.post<{ success: boolean; status: PurchaseStatus }>(`/purchases/${payload.purchaseId}/receive`, {
    items: items.map((item) => ({
      productId: item.productId,
      qtyReceived: item.qtyReceived,
      cost: item.cost,
    })),
  });
}

export { purchaseStatusOptions } from "@/features/purchases/purchases-types";
