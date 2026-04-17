import { Navigate, Outlet, useLocation } from "react-router-dom";

import { LoadingBlock } from "@/components/shared/state-blocks";
import { useAuth } from "@/features/auth/auth-context";
import type { Role } from "@/types/enums";

type ProtectedRouteProps = {
  allowedRoles?: Role[];
};

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { user, isAuthenticated, isBootstrapping } = useAuth();
  const location = useLocation();

  if (isBootstrapping) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface p-6">
        <LoadingBlock title="Loading session" description="Restoring your Smart POS session..." />
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
