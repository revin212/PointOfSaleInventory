import { USE_MOCKS } from "@/lib/env";
import * as apiImpl from "@/features/inventory/inventory-api";
import * as mockImpl from "@/features/inventory/inventory-service.mock";

export type { StockRow, StockMovement, MovementType } from "@/features/inventory/inventory-types";

const impl = USE_MOCKS ? mockImpl : apiImpl;

export const listOnHand = impl.listOnHand;
export const listMovements = impl.listMovements;
export const createStockAdjustment = impl.createStockAdjustment;

export const getStockProducts = mockImpl.getStockProducts;
