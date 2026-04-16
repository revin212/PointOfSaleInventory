import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAuth } from "@/features/auth/auth-context";
import type { Role } from "@/types/enums";

type ProtectedRouteProps = {
  allowedRoles?: Role[];
};

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
