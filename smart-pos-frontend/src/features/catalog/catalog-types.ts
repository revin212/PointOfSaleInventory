import type { PosProduct } from "@/mocks/commerce";

export type Category = {
  id: string;
  name: string;
  active: boolean;
};

export type Supplier = {
  id: string;
  name: string;
  phone: string;
  contactPerson: string;
  active: boolean;
};

export type ProductRow = PosProduct & {
  categoryId: string;
  categoryName: string;
  unit: string;
  cost: number;
  barcode?: string;
  lowStockThreshold: number;
  active: boolean;
};
