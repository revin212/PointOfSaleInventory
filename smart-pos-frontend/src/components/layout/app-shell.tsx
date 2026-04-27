import { LogOut, Menu, X } from "lucide-react";
import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { AppLogo } from "@/components/shared/app-logo";
import { Button } from "@/components/ui/button";
import { APP_NAV_ITEMS } from "@/constants/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/utils";

export function AppShell() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const role = user?.role;
  const navItems = APP_NAV_ITEMS.filter((item) => role && item.roles.includes(role));
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  useEffect(() => {
    if (!mobileNavOpen) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setMobileNavOpen(false);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [mobileNavOpen]);

  return (
    <div className="flex min-h-screen bg-surface">
      <aside className="hidden w-72 shrink-0 border-r border-outline-variant/20 bg-surface-container-low p-4 lg:block">
        <div className="rounded-xl border border-outline-variant/20 bg-surface-container-lowest p-4 shadow-card">
          <AppLogo />
        </div>
        <nav className="mt-4 space-y-1">
          {navItems.length ? (
            navItems.map((item) => (
              <NavLink
                key={item.href}
                to={item.href}
                className={({ isActive }) =>
                  cn(
                    "group flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-semibold transition-colors",
                    isActive
                      ? "bg-primary/10 text-primary shadow-sm"
                      : "text-on-surface-variant hover:bg-surface-container-lowest hover:text-on-surface",
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))
          ) : (
            <div className="rounded-xl border border-outline-variant/20 bg-surface-container-lowest p-4 text-sm text-on-surface-variant shadow-card">
              No modules available for this role.
            </div>
          )}
        </nav>
      </aside>

      {mobileNavOpen ? (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation">
          <button
            type="button"
            className="absolute inset-0 bg-on-surface/20 backdrop-blur-sm"
            aria-label="Close navigation"
            onClick={() => setMobileNavOpen(false)}
          />
          <aside className="relative h-full w-[18rem] max-w-[85vw] border-r border-outline-variant/20 bg-surface-container-low p-4 shadow-popover">
            <div className="flex items-start justify-between gap-2">
              <div className="rounded-xl border border-outline-variant/20 bg-surface-container-lowest p-4 shadow-card">
                <AppLogo />
              </div>
              <Button type="button" variant="ghost" size="icon" onClick={() => setMobileNavOpen(false)} aria-label="Close menu">
                <X className="h-5 w-5" />
              </Button>
            </div>
            <nav className="mt-4 space-y-1">
              {navItems.length ? (
                navItems.map((item) => (
                  <NavLink
                    key={item.href}
                    to={item.href}
                    onClick={() => setMobileNavOpen(false)}
                    className={({ isActive }) =>
                      cn(
                        "group flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-semibold transition-colors",
                        isActive
                          ? "bg-primary/10 text-primary shadow-sm"
                          : "text-on-surface-variant hover:bg-surface-container-lowest hover:text-on-surface",
                      )
                    }
                  >
                    <item.icon className="h-4 w-4" />
                    {item.label}
                  </NavLink>
                ))
              ) : (
                <div className="rounded-xl border border-outline-variant/20 bg-surface-container-lowest p-4 text-sm text-on-surface-variant shadow-card">
                  No modules available for this role.
                </div>
              )}
            </nav>
          </aside>
        </div>
      ) : null}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 border-b border-outline-variant/20 bg-surface/85 backdrop-blur supports-[backdrop-filter]:bg-surface/75">
          <div className="app-container flex items-center justify-between gap-3 px-4 py-4 lg:px-6">
            <button
              className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-outline-variant/20 bg-surface-container-low shadow-sm transition-colors hover:bg-surface-container-highest lg:hidden"
              type="button"
              aria-label="Open navigation"
              onClick={() => setMobileNavOpen(true)}
            >
              <Menu className="h-5 w-5" />
            </button>
            <div className="flex items-center gap-2">
              <div className="rounded-xl border border-outline-variant/20 bg-surface-container-low px-3 py-2 text-sm shadow-sm">
                <p className="font-semibold text-on-surface">{user?.name ?? "Guest"}</p>
                <p className="text-xs text-on-surface-variant">{user?.role ?? "N/A"}</p>
              </div>
              <Button
                type="button"
                variant="secondary"
                size="icon"
                onClick={async () => {
                  await logout();
                  navigate("/login", { replace: true });
                }}
                aria-label="Logout"
                title="Logout"
              >
                <LogOut className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </header>
        <main className="flex-1">
          <div className="app-container p-3 sm:p-4 lg:p-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
