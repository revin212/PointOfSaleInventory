import { createBrowserRouter, Navigate } from "react-router-dom";

import { AppShell } from "@/components/layout/app-shell";
import { LoginPage } from "@/features/auth/login-page";
import { CategoriesPage } from "@/features/catalog/categories-page";
import { ProductsPage } from "@/features/catalog/products-page";
import { SuppliersPage } from "@/features/catalog/suppliers-page";
import { FoundationPage } from "@/features/foundation/foundation-page";
import { InventoryPage } from "@/features/inventory/inventory-page";
import { StockAdjustmentsPage } from "@/features/inventory/stock-adjustments-page";
import { PosPage } from "@/features/pos/pos-page";
import { PurchaseReceivePage } from "@/features/purchases/purchase-receive-page";
import { PurchasesPage } from "@/features/purchases/purchases-page";
import { ReportsPage } from "@/features/reports/reports-page";
import { SaleDetailPage } from "@/features/sales/sale-detail-page";
import { SalesPage } from "@/features/sales/sales-page";
import { UsersPage } from "@/features/users/users-page";
import { ProtectedRoute } from "@/router/route-guards";
import { ROLE } from "@/types/enums";

export const appRouter = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: "/dashboard", element: <FoundationPage /> },
          { path: "/pos", element: <PosPage /> },
          { path: "/sales", element: <SalesPage /> },
          { path: "/sales/:id", element: <SaleDetailPage /> },
          { path: "/products", element: <ProductsPage /> },
          { path: "/reports", element: <ReportsPage /> },
          {
            element: <ProtectedRoute allowedRoles={[ROLE.OWNER, ROLE.WAREHOUSE]} />,
            children: [
              { path: "/categories", element: <CategoriesPage /> },
              { path: "/suppliers", element: <SuppliersPage /> },
              { path: "/purchases", element: <PurchasesPage /> },
              { path: "/purchases/:id/receive", element: <PurchaseReceivePage /> },
              { path: "/inventory", element: <InventoryPage /> },
              { path: "/stock-adjustments", element: <StockAdjustmentsPage /> },
            ],
          },
          {
            element: <ProtectedRoute allowedRoles={[ROLE.OWNER]} />,
            children: [{ path: "/users", element: <UsersPage /> }],
          },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/dashboard" replace /> },
]);
