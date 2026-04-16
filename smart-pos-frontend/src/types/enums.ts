export const ROLE = {
  OWNER: "OWNER",
  CASHIER: "CASHIER",
  WAREHOUSE: "WAREHOUSE",
} as const;

export type Role = (typeof ROLE)[keyof typeof ROLE];

export const PAYMENT_METHOD = {
  CASH: "CASH",
  TRANSFER: "TRANSFER",
  EWALLET: "EWALLET",
} as const;

export type PaymentMethod = (typeof PAYMENT_METHOD)[keyof typeof PAYMENT_METHOD];
