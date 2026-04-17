import { MOCK_PRODUCTS } from "@/mocks/commerce";
import type { Category, ProductRow, Supplier } from "@/features/catalog/catalog-types";

export type { Category, ProductRow, Supplier };

let categories: Category[] = [
  { id: "c-1", name: "Coffee Beans", active: true },
  { id: "c-2", name: "Brewing Tools", active: true },
  { id: "c-3", name: "Packaging", active: true },
];

let suppliers: Supplier[] = [
  { id: "sup-1", name: "Nusantara Coffee Supply", phone: "081212345678", contactPerson: "Rudi", active: true },
  { id: "sup-2", name: "Brew Tools Indonesia", phone: "081298761234", contactPerson: "Sari", active: true },
];

let products: ProductRow[] = MOCK_PRODUCTS.map((product, index) => ({
  ...product,
  categoryId: index % 2 === 0 ? "c-1" : "c-2",
  categoryName: index % 2 === 0 ? "Coffee Beans" : "Brewing Tools",
  unit: "pcs",
  cost: Math.floor(product.price * 0.65),
  barcode: `BAR-${product.sku}`,
  lowStockThreshold: 10,
  active: true,
}));

function sleep(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export async function listProducts(params: {
  query?: string;
  categoryId?: string;
  page?: number;
  size?: number;
}): Promise<{ content: ProductRow[]; page: number; size: number; totalElements: number; totalPages: number }> {
  await sleep(350);
  const page = params.page ?? 0;
  const size = params.size ?? 10;
  let filtered = products;

  if (params.query) {
    const q = params.query.toLowerCase();
    filtered = filtered.filter(
      (item) => item.name.toLowerCase().includes(q) || item.sku.toLowerCase().includes(q) || (item.barcode ?? "").toLowerCase().includes(q),
    );
  }
  if (params.categoryId && params.categoryId !== "ALL") {
    filtered = filtered.filter((item) => item.categoryId === params.categoryId);
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

export async function createProduct(payload: Omit<ProductRow, "id">): Promise<ProductRow> {
  await sleep(300);
  const created: ProductRow = { ...payload, id: `prod-${Date.now()}` };
  products = [created, ...products];
  return created;
}

export async function updateProduct(id: string, payload: Omit<ProductRow, "id">): Promise<ProductRow> {
  await sleep(300);
  const target = products.find((item) => item.id === id);
  if (!target) {
    throw new Error("Product not found.");
  }
  const updated: ProductRow = { ...payload, id };
  products = products.map((item) => (item.id === id ? updated : item));
  return updated;
}

export async function deleteProduct(id: string): Promise<{ success: boolean }> {
  await sleep(250);
  products = products.filter((item) => item.id !== id);
  return { success: true };
}

export async function listCategories(): Promise<Category[]> {
  await sleep(250);
  return [...categories];
}

export async function createCategory(payload: Omit<Category, "id">): Promise<Category> {
  await sleep(250);
  const duplicate = categories.find((item) => item.name.toLowerCase() === payload.name.toLowerCase());
  if (duplicate) {
    throw new Error("Category name already exists.");
  }
  const created: Category = { ...payload, id: `cat-${Date.now()}` };
  categories = [created, ...categories];
  return created;
}

export async function updateCategory(id: string, payload: Omit<Category, "id">): Promise<Category> {
  await sleep(250);
  const found = categories.find((item) => item.id === id);
  if (!found) {
    throw new Error("Category not found.");
  }
  const updated: Category = { ...payload, id };
  categories = categories.map((item) => (item.id === id ? updated : item));
  return updated;
}

export async function deleteCategory(id: string): Promise<{ success: boolean }> {
  await sleep(250);
  categories = categories.filter((item) => item.id !== id);
  return { success: true };
}

export async function listSuppliers(): Promise<Supplier[]> {
  await sleep(250);
  return [...suppliers];
}

export async function createSupplier(payload: Omit<Supplier, "id">): Promise<Supplier> {
  await sleep(250);
  const created: Supplier = { ...payload, id: `sup-${Date.now()}` };
  suppliers = [created, ...suppliers];
  return created;
}

export async function updateSupplier(id: string, payload: Omit<Supplier, "id">): Promise<Supplier> {
  await sleep(250);
  const found = suppliers.find((item) => item.id === id);
  if (!found) {
    throw new Error("Supplier not found.");
  }
  const updated: Supplier = { ...payload, id };
  suppliers = suppliers.map((item) => (item.id === id ? updated : item));
  return updated;
}

export async function deleteSupplier(id: string): Promise<{ success: boolean }> {
  await sleep(250);
  suppliers = suppliers.filter((item) => item.id !== id);
  return { success: true };
}
