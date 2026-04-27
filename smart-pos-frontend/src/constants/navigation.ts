import type { LucideIcon } from "lucide-react";
import {
  BarChart3,
  Boxes,
  CreditCard,
  LayoutDashboard,
  PackageSearch,
  PieChart,
  ReceiptText,
  ShoppingBasket,
  ShoppingCart,
  UserCog,
  Users,
} from "lucide-react";

import { ROLE, type Role } from "@/types/enums";

export type AppNavItem = {
  label: string;
  href: string;
  icon: LucideIcon;
  roles: Role[];
};

export const APP_NAV_ITEMS: AppNavItem[] = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard, roles: [ROLE.OWNER, ROLE.CASHIER, ROLE.WAREHOUSE] },
  { label: "POS", href: "/pos", icon: ShoppingCart, roles: [ROLE.OWNER, ROLE.CASHIER] },
  { label: "Sales", href: "/sales", icon: ReceiptText, roles: [ROLE.OWNER, ROLE.CASHIER] },
  { label: "Products", href: "/products", icon: Boxes, roles: [ROLE.OWNER, ROLE.CASHIER, ROLE.WAREHOUSE] },
  { label: "Categories", href: "/categories", icon: PackageSearch, roles: [ROLE.OWNER, ROLE.WAREHOUSE] },
  { label: "Suppliers", href: "/suppliers", icon: Users, roles: [ROLE.OWNER, ROLE.WAREHOUSE] },
  { label: "Purchases", href: "/purchases", icon: ShoppingBasket, roles: [ROLE.OWNER, ROLE.WAREHOUSE] },
  { label: "Inventory", href: "/inventory", icon: BarChart3, roles: [ROLE.OWNER, ROLE.WAREHOUSE] },
  { label: "Reports", href: "/reports", icon: PieChart, roles: [ROLE.OWNER] },
  { label: "Payment Types", href: "/payment-types", icon: CreditCard, roles: [ROLE.OWNER] },
  { label: "Users & Roles", href: "/users", icon: UserCog, roles: [ROLE.OWNER] },
];
