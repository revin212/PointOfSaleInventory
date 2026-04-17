import { USE_MOCKS } from "@/lib/env";
import * as apiImpl from "@/features/catalog/catalog-api";
import * as mockImpl from "@/features/catalog/catalog-service.mock";

export type { Category, ProductRow, Supplier } from "@/features/catalog/catalog-types";

const impl = USE_MOCKS ? mockImpl : apiImpl;

export const listProducts = impl.listProducts;
export const createProduct = impl.createProduct;
export const updateProduct = impl.updateProduct;
export const deleteProduct = impl.deleteProduct;
export const listCategories = impl.listCategories;
export const createCategory = impl.createCategory;
export const updateCategory = impl.updateCategory;
export const deleteCategory = impl.deleteCategory;
export const listSuppliers = impl.listSuppliers;
export const createSupplier = impl.createSupplier;
export const updateSupplier = impl.updateSupplier;
export const deleteSupplier = impl.deleteSupplier;
