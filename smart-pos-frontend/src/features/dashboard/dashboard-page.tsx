import { useQuery } from "@tanstack/react-query";
import { PackageSearch, ReceiptText, Warehouse, Wallet } from "lucide-react";

import { KpiCard } from "@/components/shared/kpi-card";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { StockBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";
import { getDashboardData } from "@/features/dashboard/dashboard-service";
import { formatIDR } from "@/lib/format";
import { USE_MOCKS } from "@/lib/env";
import { ROLE } from "@/types/enums";

export function DashboardPage() {
  const { user } = useAuth();
  const role = user?.role;

  const dashboardQuery = useQuery({
    queryKey: ["dashboard", role],
    queryFn: () => getDashboardData(role!),
    enabled: Boolean(role),
  });

  return (
    <div className="space-y-4 md:space-y-6">
      <PageHeader
        title="Dashboard"
        subtitle="Daily KPI, recent sales, and inventory insights."
        actions={
          <Button variant="secondary" onClick={() => dashboardQuery.refetch()} disabled={dashboardQuery.isFetching}>
            {dashboardQuery.isFetching ? "Refreshing..." : "Refresh"}
          </Button>
        }
      />

      {dashboardQuery.isLoading ? <LoadingBlock title="Loading dashboard" description="Fetching KPI and activity data..." /> : null}

      {dashboardQuery.isError ? (
        <ErrorBlock
          title="Failed to load dashboard"
          description={(dashboardQuery.error as Error).message || "Unable to load dashboard data."}
          onRetry={() => dashboardQuery.refetch()}
        />
      ) : null}

      {dashboardQuery.isSuccess ? (
        <>
          {USE_MOCKS ? (
            <SuccessBlock title="Demo dashboard data loaded" description="Dashboard is running in mock mode." />
          ) : null}

          <div className="grid gap-4 md:grid-cols-3">
            {role === ROLE.WAREHOUSE ? (
              <>
                <KpiCard label="Stock SKUs" value={String(dashboardQuery.data.kpis.skuCount)} icon={Warehouse} />
                <KpiCard label="Out of stock" value={String(dashboardQuery.data.kpis.outOfStockCount)} icon={PackageSearch} />
                <KpiCard label="Low stock items" value={String(dashboardQuery.data.kpis.lowStockCount)} icon={PackageSearch} />
              </>
            ) : (
              <>
                <KpiCard
                  label="Today's Sales"
                  value={formatIDR(dashboardQuery.data.kpis.todaySales)}
                  trend="+12.5% vs yesterday"
                  icon={Wallet}
                />
                <KpiCard label="Transactions" value={String(dashboardQuery.data.kpis.transactionCount)} icon={ReceiptText} />
                <KpiCard label="Low Stock Items" value={String(dashboardQuery.data.kpis.lowStockCount)} icon={PackageSearch} />
              </>
            )}
          </div>

          <div className="grid gap-4 md:gap-6 xl:grid-cols-[1.15fr_0.85fr]">
            <Card className="space-y-3">
              <h2 className="text-lg font-bold">{role === ROLE.WAREHOUSE ? "Recent Stock Movements" : "Recent Sales"}</h2>
              {role === ROLE.WAREHOUSE ? (
                dashboardQuery.data.recentMovements.length === 0 ? (
                  <EmptyBlock title="No recent movements" description="No stock movements recorded yet." />
                ) : (
                  <div className="space-y-2">
                    {dashboardQuery.data.recentMovements.map((m) => (
                      <div key={m.id} className="rounded-xl bg-surface-container-low p-3">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="truncate font-semibold">{m.productName}</p>
                            <p className="truncate text-xs text-on-surface-variant">
                              {m.type} • {m.reason}
                            </p>
                            <p className="text-xs text-on-surface-variant">{new Date(m.createdAt).toLocaleString("id-ID")}</p>
                          </div>
                          <p className={`shrink-0 text-sm font-semibold tabular-nums-idr ${m.qtyDelta < 0 ? "text-error" : "text-primary"}`}>
                            {m.qtyDelta > 0 ? `+${m.qtyDelta}` : m.qtyDelta}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                )
              ) : dashboardQuery.data.recentSales.length === 0 ? (
                <EmptyBlock title="No recent sales" description="No transaction activity for this role/time range." />
              ) : (
                <div className="space-y-2">
                  {dashboardQuery.data.recentSales.map((sale) => (
                    <div key={sale.id} className="rounded-xl bg-surface-container-low p-3">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="font-semibold">{sale.invoiceNo}</p>
                          <p className="text-xs text-on-surface-variant">
                            {sale.cashierName} • {sale.paymentMethod}
                          </p>
                          <p className="text-xs text-on-surface-variant">{new Date(sale.createdAt).toLocaleString("id-ID")}</p>
                        </div>
                        <p className="text-sm font-semibold tabular-nums-idr">{formatIDR(sale.total)}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>

            <div className="space-y-6">
              {role !== ROLE.WAREHOUSE ? (
                <Card className="space-y-3">
                  <h2 className="text-lg font-bold">Top Products</h2>
                  {dashboardQuery.data.topProducts.length === 0 ? (
                    <EmptyBlock title="No top products" description="No product performance data yet." />
                  ) : (
                    <div className="space-y-2">
                      {dashboardQuery.data.topProducts.map((product) => (
                        <div key={product.productId} className="rounded-xl bg-surface-container-low p-3">
                          <p className="font-semibold">{product.name}</p>
                          <p className="text-xs text-on-surface-variant">{product.sku}</p>
                          <p className="text-xs text-on-surface-variant">Qty sold: {product.qtySold}</p>
                          <p className="text-sm font-semibold tabular-nums-idr">{formatIDR(product.revenue)}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </Card>
              ) : null}

              <Card className="space-y-3">
                <h2 className="text-lg font-bold">Low Stock Watchlist</h2>
                {dashboardQuery.data.lowStockItems.length === 0 ? (
                  <EmptyBlock title="No low stock alerts" description="All monitored items are above threshold." />
                ) : (
                  <div className="space-y-2">
                    {dashboardQuery.data.lowStockItems.map((item) => (
                      <div key={item.productId} className="rounded-xl bg-surface-container-low p-3">
                        <div className="flex items-center justify-between gap-3">
                          <div>
                            <p className="font-semibold">{item.name}</p>
                            <p className="text-xs text-on-surface-variant">{item.sku}</p>
                          </div>
                          <div className="text-right">
                            <p className="text-xs text-on-surface-variant">
                              {item.stock} / {item.threshold}
                            </p>
                            <StockBadge stockTone={item.stock <= 0 ? "out" : "low"} />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
