import { USE_MOCKS } from "@/lib/env";
import * as apiImpl from "@/features/purchases/purchases-api";
import * as mockImpl from "@/features/purchases/purchases-service.mock";

export type { PurchaseItem, PurchaseRecord } from "@/features/purchases/purchases-types";
export { purchaseStatusOptions } from "@/features/purchases/purchases-types";

const impl = USE_MOCKS ? mockImpl : apiImpl;

export const listPurchases = impl.listPurchases;
export const getPurchaseById = impl.getPurchaseById;
export const createPurchase = impl.createPurchase;
export const receivePurchase = impl.receivePurchase;
