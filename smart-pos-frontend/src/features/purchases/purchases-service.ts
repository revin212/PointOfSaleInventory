import { MOCK_PRODUCTS } from "@/mocks/commerce";
import type { PurchaseStatus } from "@/types/purchase";

export type PurchaseItem = {
  productId: string;
  productName: string;
  qtyOrdered: number;
  qtyReceived: number;
  cost: number;
};

export type PurchaseRecord = {
  id: string;
  purchaseNo: string;
  supplierId: string;
  supplierName: string;
  status: PurchaseStatus;
  createdAt: string;
  items: PurchaseItem[];
};

let purchases: PurchaseRecord[] = [
  {
    id: "po-1",
    purchaseNo: "PO-2026-001",
    supplierId: "sup-1",
    supplierName: "Nusantara Coffee Supply",
    status: "OPEN",
    createdAt: "2026-04-15T08:00:00.000Z",
    items: [
      { productId: "p-1", productName: "Arabica Gayo 250g", qtyOrdered: 30, qtyReceived: 10, cost: 65000 },
      { productId: "p-2", productName: "Robusta Blend 500g", qtyOrdered: 20, qtyReceived: 20, cost: 82000 },
    ],
  },
  {
    id: "po-2",
    purchaseNo: "PO-2026-002",
    supplierId: "sup-2",
    supplierName: "Brew Tools Indonesia",
    status: "PARTIALLY_RECEIVED",
    createdAt: "2026-04-15T10:30:00.000Z",
    items: [{ productId: "p-3", productName: "Manual Brew Kettle", qtyOrdered: 8, qtyReceived: 4, cost: 240000 }],
  },
];

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function deriveStatus(items: PurchaseItem[]): PurchaseStatus {
  const totalOrdered = items.reduce((sum, item) => sum + item.qtyOrdered, 0);
  const totalReceived = items.reduce((sum, item) => sum + item.qtyReceived, 0);
  if (totalReceived <= 0) return "OPEN";
  if (totalReceived >= totalOrdered) return "RECEIVED";
  return "PARTIALLY_RECEIVED";
}

export async function listPurchases(params: { status?: PurchaseStatus | "ALL"; page?: number; size?: number }) {
  await sleep(350);
  const page = params.page ?? 0;
  const size = params.size ?? 10;
  let filtered = purchases;
  if (params.status && params.status !== "ALL") {
    filtered = filtered.filter((purchase) => purchase.status === params.status);
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

export async function getPurchaseById(id: string) {
  await sleep(250);
  const purchase = purchases.find((entry) => entry.id === id);
  if (!purchase) throw new Error("Purchase not found.");
  return purchase;
}

export async function createPurchase(payload: { supplierName: string; items: Array<{ productId: string; qtyOrdered: number; cost: number }> }) {
  await sleep(350);
  const mappedItems: PurchaseItem[] = payload.items.map((item) => {
    const product = MOCK_PRODUCTS.find((entry) => entry.id === item.productId);
    return {
      productId: item.productId,
      productName: product?.name ?? "Unknown Product",
      qtyOrdered: item.qtyOrdered,
      qtyReceived: 0,
      cost: item.cost,
    };
  });
  const created: PurchaseRecord = {
    id: `po-${Date.now()}`,
    purchaseNo: `PO-${new Date().getFullYear()}-${Math.floor(Math.random() * 1000)
      .toString()
      .padStart(3, "0")}`,
    supplierId: payload.supplierName.toLowerCase().replaceAll(" ", "-"),
    supplierName: payload.supplierName,
    status: "OPEN",
    createdAt: new Date().toISOString(),
    items: mappedItems,
  };
  purchases = [created, ...purchases];
  return created;
}

export async function receivePurchase(payload: { purchaseId: string; items: Array<{ productId: string; qtyReceived: number; cost: number }> }) {
  await sleep(350);
  const purchase = purchases.find((entry) => entry.id === payload.purchaseId);
  if (!purchase) throw new Error("Purchase not found.");
  const updatedItems = purchase.items.map((item) => {
    const incoming = payload.items.find((incomingItem) => incomingItem.productId === item.productId);
    if (!incoming) return item;
    const remaining = item.qtyOrdered - item.qtyReceived;
    if (incoming.qtyReceived < 0 || incoming.qtyReceived > remaining) {
      throw new Error(`Invalid receive qty for ${item.productName}. Max allowed: ${remaining}`);
    }
    return {
      ...item,
      qtyReceived: item.qtyReceived + incoming.qtyReceived,
      cost: incoming.cost,
    };
  });
  const updated: PurchaseRecord = {
    ...purchase,
    items: updatedItems,
    status: deriveStatus(updatedItems),
  };
  purchases = purchases.map((entry) => (entry.id === updated.id ? updated : entry));
  return { success: true, status: updated.status };
}

export const purchaseStatusOptions: Array<{ label: string; value: PurchaseStatus | "ALL" }> = [
  { label: "All", value: "ALL" },
  { label: "Open", value: "OPEN" },
  { label: "Partially Received", value: "PARTIALLY_RECEIVED" },
  { label: "Received", value: "RECEIVED" },
];
