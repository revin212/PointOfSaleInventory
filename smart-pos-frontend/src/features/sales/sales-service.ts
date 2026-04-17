import { USE_MOCKS } from "@/lib/env";
import * as apiImpl from "@/features/sales/sales-api";
import * as mockImpl from "@/features/sales/sales-service.mock";

const impl = USE_MOCKS ? mockImpl : apiImpl;

export const getSales = impl.getSales;
export const getSaleById = impl.getSaleById;
export const cancelSaleById = impl.cancelSaleById;
export const createSale = impl.createSale;
export const paymentMethodOptions = impl.paymentMethodOptions;
