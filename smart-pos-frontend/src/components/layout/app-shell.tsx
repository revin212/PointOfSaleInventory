import { Menu, Search } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";

import { AppLogo } from "@/components/shared/app-logo";
import { APP_NAV_ITEMS } from "@/constants/navigation";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/utils";

export function AppShell() {
  const { user } = useAuth();
  const role = user?.role;
  const navItems = APP_NAV_ITEMS.filter((item) => role && item.roles.includes(role));

  return (
    <div className="flex min-h-screen bg-surface">
      <aside className="hidden w-64 shrink-0 bg-surface-container-low p-4 lg:block">
        <div className="rounded-xl bg-surface-container-lowest p-4">
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
                    "flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-semibold transition",
                    isActive ? "bg-primary/10 text-primary" : "text-on-surface-variant hover:bg-surface-container-lowest",
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))
          ) : (
            <div className="rounded-xl bg-surface-container-lowest p-4 text-sm text-on-surface-variant">
              No modules available for this role.
            </div>
          )}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 bg-surface/90 backdrop-blur">
          <div className="flex items-center justify-between gap-3 p-4 lg:p-6">
            <button className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-surface-container-low lg:hidden" type="button" aria-label="Open navigation">
              <Menu className="h-5 w-5" />
            </button>
            <div className="hidden w-full max-w-md items-center gap-2 rounded-xl bg-surface-container-low px-3 py-2 lg:flex">
              <Search className="h-4 w-4 text-on-surface-variant" />
              <input
                className="w-full bg-transparent text-sm text-on-surface outline-none placeholder:text-on-surface-variant"
                placeholder="Search products, sales, inventory"
                type="search"
              />
            </div>
            <div className="rounded-xl bg-surface-container-low px-3 py-2 text-sm">
              <p className="font-semibold text-on-surface">{user?.name ?? "Guest"}</p>
              <p className="text-xs text-on-surface-variant">{user?.role ?? "N/A"}</p>
            </div>
          </div>
        </header>
        <main className="flex-1 p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
