import { PAYMENT_METHOD, type PaymentMethod } from "@/types/enums";

export type PosProduct = {
  id: string;
  sku: string;
  name: string;
  price: number;
  stock: number;
};

export type SaleItem = {
  productId: string;
  sku: string;
  name: string;
  qty: number;
  unitPrice: number;
  lineDiscount?: number;
};

export type SaleStatus = "COMPLETED" | "CANCELLED";

export type SaleRecord = {
  id: string;
  invoiceNo: string;
  createdAt: string;
  cashierName: string;
  paymentMethod: PaymentMethod;
  status: SaleStatus;
  items: SaleItem[];
  totals: {
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
};

export const MOCK_PRODUCTS: PosProduct[] = [
  { id: "p-1", sku: "SKU-1001", name: "Arabica Gayo 250g", price: 95000, stock: 25 },
  { id: "p-2", sku: "SKU-1002", name: "Robusta Blend 500g", price: 120000, stock: 15 },
  { id: "p-3", sku: "SKU-1003", name: "Manual Brew Kettle", price: 350000, stock: 6 },
  { id: "p-4", sku: "SKU-1004", name: "Paper Filter V60", price: 45000, stock: 42 },
  { id: "p-5", sku: "SKU-1005", name: "Cold Brew Bottle", price: 78000, stock: 18 },
];

export const MOCK_SALES: SaleRecord[] = [
  {
    id: "s-1",
    invoiceNo: "INV-2026-0001",
    createdAt: "2026-04-16T08:30:00.000Z",
    cashierName: "Cashier User",
    paymentMethod: PAYMENT_METHOD.CASH,
    status: "COMPLETED",
    items: [
      { productId: "p-1", sku: "SKU-1001", name: "Arabica Gayo 250g", qty: 2, unitPrice: 95000 },
      { productId: "p-4", sku: "SKU-1004", name: "Paper Filter V60", qty: 1, unitPrice: 45000 },
    ],
    totals: { subtotal: 235000, discount: 10000, total: 225000, paidAmount: 250000, changeAmount: 25000 },
  },
  {
    id: "s-2",
    invoiceNo: "INV-2026-0002",
    createdAt: "2026-04-16T09:10:00.000Z",
    cashierName: "Cashier User",
    paymentMethod: PAYMENT_METHOD.TRANSFER,
    status: "COMPLETED",
    items: [{ productId: "p-3", sku: "SKU-1003", name: "Manual Brew Kettle", qty: 1, unitPrice: 350000 }],
    totals: { subtotal: 350000, discount: 0, total: 350000, paidAmount: 350000, changeAmount: 0 },
  },
  {
    id: "s-3",
    invoiceNo: "INV-2026-0003",
    createdAt: "2026-04-16T09:45:00.000Z",
    cashierName: "Owner User",
    paymentMethod: PAYMENT_METHOD.EWALLET,
    status: "CANCELLED",
    items: [{ productId: "p-2", sku: "SKU-1002", name: "Robusta Blend 500g", qty: 1, unitPrice: 120000 }],
    totals: { subtotal: 120000, discount: 0, total: 120000, paidAmount: 120000, changeAmount: 0 },
  },
  {
    id: "s-4",
    invoiceNo: "INV-2026-0004",
    createdAt: "2026-04-16T11:20:00.000Z",
    cashierName: "Cashier User",
    paymentMethod: PAYMENT_METHOD.CASH,
    status: "COMPLETED",
    items: [
      { productId: "p-5", sku: "SKU-1005", name: "Cold Brew Bottle", qty: 3, unitPrice: 78000, lineDiscount: 10000 },
    ],
    totals: { subtotal: 234000, discount: 10000, total: 224000, paidAmount: 250000, changeAmount: 26000 },
  },
];
