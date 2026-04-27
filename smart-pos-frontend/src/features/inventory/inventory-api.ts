import { api, type PageResponse } from "@/lib/api-client";
import type { MovementType, StockMovement, StockRow } from "@/features/inventory/inventory-types";

type BackendOnHand = {
  productId: string;
  sku: string;
  name: string;
  categoryId?: string | null;
  categoryName?: string | null;
  unit: string;
  onHand: number;
  lowStockThreshold: number;
  lowStock: boolean;
  active: boolean;
};

type BackendMovement = {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  type: MovementType;
  qtyDelta: number;
  unitCost?: number | null;
  refType?: string | null;
  refId?: string | null;
  note?: string | null;
  createdBy?: string | null;
  createdByName?: string | null;
  createdAt: string;
};

function mapOnHand(raw: BackendOnHand): StockRow {
  return {
    productId: raw.productId,
    sku: raw.sku,
    name: raw.name,
    stock: raw.onHand,
    lowStockThreshold: raw.lowStockThreshold,
    unit: raw.unit,
  };
}

function mapMovement(raw: BackendMovement): StockMovement {
  return {
    id: raw.id,
    productId: raw.productId,
    productName: raw.productName,
    type: raw.type,
    qtyDelta: raw.qtyDelta,
    reason: raw.note ?? raw.refType ?? raw.type,
    createdAt: raw.createdAt,
  };
}

export async function listOnHand(params: {
  query?: string;
  lowOnly?: boolean;
  page?: number;
  size?: number;
}): Promise<{ content: StockRow[]; page: number; size: number; totalElements: number; totalPages: number }> {
  const response = await api.get<PageResponse<BackendOnHand>>("/stock/on-hand", {
    query: {
      query: params.query,
      lowOnly: params.lowOnly ? true : undefined,
      page: params.page,
      size: params.size,
    },
  });
  return {
    content: response.content.map(mapOnHand),
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
  };
}

export async function listMovements(params: {
  productId?: string;
  type?: MovementType;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}): Promise<{ content: StockMovement[]; page: number; size: number; totalElements: number; totalPages: number }> {
  const response = await api.get<PageResponse<BackendMovement>>("/stock/movements", {
    query: {
      productId: params.productId && params.productId !== "ALL" ? params.productId : undefined,
      type: params.type,
      from: params.from,
      to: params.to,
      page: params.page,
      size: params.size,
    },
  });
  return {
    content: response.content.map(mapMovement),
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
  };
}

export async function createStockAdjustment(payload: {
  productId: string;
  qtyDelta: number;
  reason: string;
}): Promise<{ success: boolean }> {
  await api.post<unknown>("/stock/adjustments", {
    productId: payload.productId,
    qtyDelta: payload.qtyDelta,
    note: payload.reason,
  });
  return { success: true };
}
