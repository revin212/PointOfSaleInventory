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

export const purchaseStatusOptions: Array<{ label: string; value: PurchaseStatus | "ALL" }> = [
  { label: "All", value: "ALL" },
  { label: "Open", value: "OPEN" },
  { label: "Partially Received", value: "PARTIALLY_RECEIVED" },
  { label: "Received", value: "RECEIVED" },
];
