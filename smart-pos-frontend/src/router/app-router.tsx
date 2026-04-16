import { createBrowserRouter, Navigate } from "react-router-dom";

import { AppShell } from "@/components/layout/app-shell";
import { LoginPage } from "@/features/auth/login-page";
import { FoundationPage } from "@/features/foundation/foundation-page";
import { ModulePlaceholderPage } from "@/features/placeholder/module-placeholder-page";
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
          { path: "/pos", element: <ModulePlaceholderPage moduleName="POS" /> },
          { path: "/sales", element: <ModulePlaceholderPage moduleName="Sales" /> },
          { path: "/products", element: <ModulePlaceholderPage moduleName="Products" /> },
          { path: "/categories", element: <ModulePlaceholderPage moduleName="Categories" /> },
          { path: "/suppliers", element: <ModulePlaceholderPage moduleName="Suppliers" /> },
          { path: "/purchases", element: <ModulePlaceholderPage moduleName="Purchases" /> },
          { path: "/inventory", element: <ModulePlaceholderPage moduleName="Inventory" /> },
          { path: "/reports", element: <ModulePlaceholderPage moduleName="Reports" /> },
        ],
      },
      {
        element: <ProtectedRoute allowedRoles={[ROLE.OWNER]} />,
        children: [{ path: "/users", element: <ModulePlaceholderPage moduleName="Users & Roles" /> }],
      },
    ],
  },
  { path: "*", element: <Navigate to="/dashboard" replace /> },
]);
