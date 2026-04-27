import { api, type PageResponse } from "@/lib/api-client";
import type { Category, ProductRow, Supplier } from "@/features/catalog/catalog-types";

type BackendCategory = {
  id: string;
  name: string;
};

type BackendSupplier = {
  id: string;
  name: string;
  phone?: string | null;
  address?: string | null;
};

type BackendProduct = {
  id: string;
  sku: string;
  name: string;
  categoryId?: string | null;
  categoryName?: string | null;
  supplierId?: string | null;
  supplierName?: string | null;
  unit: string;
  cost: number;
  price: number;
  barcode?: string | null;
  lowStockThreshold: number;
  active: boolean;
};

type ProductsPage = PageResponse<BackendProduct>;

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

function mapCategory(raw: BackendCategory): Category {
  return {
    id: raw.id,
    name: raw.name,
    active: true,
  };
}

function mapSupplier(raw: BackendSupplier): Supplier {
  return {
    id: raw.id,
    name: raw.name,
    phone: raw.phone ?? "",
    contactPerson: raw.address ?? "",
    active: true,
  };
}

function mapProduct(raw: BackendProduct, onHandByProductId: Map<string, number>): ProductRow {
  return {
    id: raw.id,
    sku: raw.sku,
    name: raw.name,
    price: Number(raw.price ?? 0),
    stock: onHandByProductId.get(raw.id) ?? 0,
    categoryId: raw.categoryId ?? "",
    categoryName: raw.categoryName ?? "Uncategorized",
    supplierId: raw.supplierId ?? undefined,
    supplierName: raw.supplierName ?? undefined,
    unit: raw.unit,
    cost: Number(raw.cost ?? 0),
    barcode: raw.barcode ?? undefined,
    lowStockThreshold: raw.lowStockThreshold ?? 0,
    active: raw.active,
  };
}

export async function listProducts(params: {
  query?: string;
  categoryId?: string;
  page?: number;
  size?: number;
}): Promise<{ content: ProductRow[]; page: number; size: number; totalElements: number; totalPages: number }> {
  const [productsResponse, onHandResponse] = await Promise.all([
    api.get<ProductsPage>("/products", {
      query: {
        query: params.query,
        categoryId: params.categoryId && params.categoryId !== "ALL" ? params.categoryId : undefined,
        page: params.page,
        size: params.size,
      },
    }),
    api.get<PageResponse<BackendOnHand>>("/stock/on-hand", {
      query: {
        query: params.query,
        categoryId: params.categoryId && params.categoryId !== "ALL" ? params.categoryId : undefined,
        lowOnly: false,
        page: params.page,
        size: params.size,
      },
    }),
  ]);

  const onHandByProductId = new Map<string, number>();
  for (const row of onHandResponse.content) {
    onHandByProductId.set(row.productId, row.onHand);
  }

  return {
    content: productsResponse.content.map((raw) => mapProduct(raw, onHandByProductId)),
    page: productsResponse.page,
    size: productsResponse.size,
    totalElements: productsResponse.totalElements,
    totalPages: productsResponse.totalPages,
  };
}

export async function createProduct(payload: Omit<ProductRow, "id">): Promise<ProductRow> {
  const body = {
    sku: payload.sku,
    name: payload.name,
    categoryId: payload.categoryId || null,
    supplierId: payload.supplierId || null,
    unit: payload.unit,
    cost: payload.cost,
    price: payload.price,
    barcode: payload.barcode || null,
    lowStockThreshold: payload.lowStockThreshold,
    active: payload.active,
  };
  const raw = await api.post<BackendProduct>("/products", body);
  return mapProduct(raw, new Map());
}

export async function updateProduct(id: string, payload: Omit<ProductRow, "id">): Promise<ProductRow> {
  const body = {
    sku: payload.sku,
    name: payload.name,
    categoryId: payload.categoryId || null,
    supplierId: payload.supplierId || null,
    unit: payload.unit,
    cost: payload.cost,
    price: payload.price,
    barcode: payload.barcode || null,
    lowStockThreshold: payload.lowStockThreshold,
    active: payload.active,
  };
  const raw = await api.put<BackendProduct>(`/products/${id}`, body);
  return mapProduct(raw, new Map());
}

export async function deleteProduct(id: string): Promise<{ success: boolean }> {
  await api.delete<{ success: boolean }>(`/products/${id}`);
  return { success: true };
}

export async function listCategories(): Promise<Category[]> {
  const response = await api.get<PageResponse<BackendCategory>>("/categories", {
    query: { size: 200 },
  });
  return response.content.map(mapCategory);
}

export async function createCategory(payload: Omit<Category, "id">): Promise<Category> {
  const raw = await api.post<BackendCategory>("/categories", { name: payload.name });
  return mapCategory(raw);
}

export async function updateCategory(id: string, payload: Omit<Category, "id">): Promise<Category> {
  const raw = await api.put<BackendCategory>(`/categories/${id}`, { name: payload.name });
  return mapCategory(raw);
}

export async function deleteCategory(id: string): Promise<{ success: boolean }> {
  await api.delete<{ success: boolean }>(`/categories/${id}`);
  return { success: true };
}

export async function listSuppliers(): Promise<Supplier[]> {
  const response = await api.get<PageResponse<BackendSupplier>>("/suppliers", {
    query: { size: 200 },
  });
  return response.content.map(mapSupplier);
}

export async function createSupplier(payload: Omit<Supplier, "id">): Promise<Supplier> {
  const raw = await api.post<BackendSupplier>("/suppliers", {
    name: payload.name,
    phone: payload.phone || null,
    address: payload.contactPerson || null,
  });
  return mapSupplier(raw);
}

export async function updateSupplier(id: string, payload: Omit<Supplier, "id">): Promise<Supplier> {
  const raw = await api.put<BackendSupplier>(`/suppliers/${id}`, {
    name: payload.name,
    phone: payload.phone || null,
    address: payload.contactPerson || null,
  });
  return mapSupplier(raw);
}

export async function deleteSupplier(id: string): Promise<{ success: boolean }> {
  await api.delete<{ success: boolean }>(`/suppliers/${id}`);
  return { success: true };
}
