export type StockRow = {
  productId: string;
  sku: string;
  name: string;
  stock: number;
  lowStockThreshold: number;
  unit: string;
};

export type MovementType = "PURCHASE_RECEIVE" | "SALE" | "ADJUSTMENT" | "SALE_CANCEL";

export type StockMovement = {
  id: string;
  productId: string;
  productName: string;
  type: MovementType;
  qtyDelta: number;
  reason: string;
  createdAt: string;
};
