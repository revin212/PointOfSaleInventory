import { api } from "@/lib/api-client";
import type { PaymentMethod } from "@/types/enums";

export type PaymentType = {
  id: string;
  method: PaymentMethod;
  name: string;
  adminFee: number;
  active: boolean;
};

export async function listPaymentTypes(): Promise<PaymentType[]> {
  const data = await api.get<PaymentType[]>("/payment-types");
  return data.map((pt) => ({
    ...pt,
    adminFee: Number(pt.adminFee ?? 0),
    active: Boolean(pt.active),
  }));
}

export async function listPaymentTypesManage(): Promise<PaymentType[]> {
  const data = await api.get<PaymentType[]>("/payment-types/manage");
  return data.map((pt) => ({
    ...pt,
    adminFee: Number(pt.adminFee ?? 0),
    active: Boolean(pt.active),
  }));
}

export async function upsertPaymentType(payload: {
  method: PaymentMethod;
  name: string;
  adminFee: number;
  active: boolean;
}): Promise<PaymentType> {
  const pt = await api.put<PaymentType>("/payment-types", payload);
  return { ...pt, adminFee: Number(pt.adminFee ?? 0), active: Boolean(pt.active) };
}

export async function updatePaymentType(id: string, payload: {
  method: PaymentMethod;
  name: string;
  adminFee: number;
  active: boolean;
}): Promise<PaymentType> {
  const pt = await api.put<PaymentType>(`/payment-types/${id}`, payload);
  return { ...pt, adminFee: Number(pt.adminFee ?? 0), active: Boolean(pt.active) };
}

