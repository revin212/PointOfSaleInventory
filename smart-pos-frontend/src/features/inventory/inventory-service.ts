import { MOCK_PRODUCTS } from "@/mocks/commerce";

type StockRow = {
  productId: string;
  sku: string;
  name: string;
  stock: number;
  lowStockThreshold: number;
  unit: string;
};

type MovementType = "PURCHASE_RECEIVE" | "SALE" | "ADJUSTMENT" | "SALE_CANCEL";

type StockMovement = {
  id: string;
  productId: string;
  productName: string;
  type: MovementType;
  qtyDelta: number;
  reason: string;
  createdAt: string;
};

const stockRows: StockRow[] = MOCK_PRODUCTS.map((product) => ({
  productId: product.id,
  sku: product.sku,
  name: product.name,
  stock: product.stock,
  lowStockThreshold: 10,
  unit: "pcs",
}));

let stockMovements: StockMovement[] = [
  {
    id: "mov-1",
    productId: "p-1",
    productName: "Arabica Gayo 250g",
    type: "PURCHASE_RECEIVE",
    qtyDelta: 10,
    reason: "Initial receive",
    createdAt: "2026-04-16T08:10:00.000Z",
  },
  {
    id: "mov-2",
    productId: "p-1",
    productName: "Arabica Gayo 250g",
    type: "SALE",
    qtyDelta: -2,
    reason: "Sale INV-2026-0001",
    createdAt: "2026-04-16T08:32:00.000Z",
  },
];

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function listOnHand(params: { query?: string; lowOnly?: boolean; page?: number; size?: number }) {
  await sleep(300);
  const page = params.page ?? 0;
  const size = params.size ?? 10;

  let filtered = stockRows;
  if (params.query) {
    const q = params.query.toLowerCase();
    filtered = filtered.filter((row) => row.name.toLowerCase().includes(q) || row.sku.toLowerCase().includes(q));
  }
  if (params.lowOnly) {
    filtered = filtered.filter((row) => row.stock <= row.lowStockThreshold);
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

export async function listMovements(params: { productId?: string; page?: number; size?: number }) {
  await sleep(300);
  const page = params.page ?? 0;
  const size = params.size ?? 10;
  let filtered = stockMovements;
  if (params.productId && params.productId !== "ALL") {
    filtered = filtered.filter((entry) => entry.productId === params.productId);
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

export async function createStockAdjustment(payload: { productId: string; qtyDelta: number; reason: string }) {
  await sleep(300);
  const row = stockRows.find((entry) => entry.productId === payload.productId);
  if (!row) {
    throw new Error("Product not found.");
  }
  const nextStock = row.stock + payload.qtyDelta;
  if (nextStock < 0) {
    throw new Error("Adjustment cannot produce negative stock.");
  }
  row.stock = nextStock;
  stockMovements = [
    {
      id: `mov-${Date.now()}`,
      productId: row.productId,
      productName: row.name,
      type: "ADJUSTMENT",
      qtyDelta: payload.qtyDelta,
      reason: payload.reason,
      createdAt: new Date().toISOString(),
    },
    ...stockMovements,
  ];
  return { success: true };
}

export function getStockProducts() {
  return stockRows.map((row) => ({ productId: row.productId, name: row.name, sku: row.sku }));
}
