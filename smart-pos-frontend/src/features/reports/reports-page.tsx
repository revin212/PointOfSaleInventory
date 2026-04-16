import { useState } from "react";
import { useQuery } from "@tanstack/react-query";

import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "@/components/shared/state-blocks";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";
import { getDailyReport, getTopProductsReport } from "@/features/reports/reports-service";
import { formatIDR } from "@/lib/format";
import { ROLE } from "@/types/enums";

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

export function ReportsPage() {
  const { user } = useAuth();
  const canView = user?.role === ROLE.OWNER;
  const [date, setDate] = useState(todayIsoDate());
  const [from, setFrom] = useState(todayIsoDate());
  const [to, setTo] = useState(todayIsoDate());

  const dailyQuery = useQuery({
    queryKey: ["report-daily", date],
    queryFn: () => getDailyReport(date),
    enabled: canView,
  });
  const topProductsQuery = useQuery({
    queryKey: ["report-top-products", from, to],
    queryFn: () => getTopProductsReport(from, to),
    enabled: canView,
  });

  if (!canView) {
    return <EmptyBlock title="Access denied" description="Reports page is owner-only in v1 scope." />;
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Reports" subtitle="Daily summary and top products based on selected date range." />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Daily Summary</h2>
          <div className="flex items-end gap-2">
            <label className="text-sm text-on-surface-variant">
              Date
              <input
                className="mt-1 block h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
                type="date"
                value={date}
                onChange={(event) => setDate(event.target.value)}
              />
            </label>
            <Button variant="secondary" onClick={() => dailyQuery.refetch()}>
              Refresh
            </Button>
          </div>
          {dailyQuery.isLoading ? <LoadingBlock title="Loading daily report" description="Fetching daily summary data..." /> : null}
          {dailyQuery.isError ? (
            <ErrorBlock title="Failed to load daily summary" description={(dailyQuery.error as Error).message} onRetry={() => dailyQuery.refetch()} />
          ) : null}
          {dailyQuery.isSuccess ? (
            <div className="space-y-2 rounded-xl bg-surface-container-low p-3">
              <p className="flex items-center justify-between">
                <span>Gross</span>
                <span className="font-semibold tabular-nums-idr">{formatIDR(dailyQuery.data.gross)}</span>
              </p>
              <p className="flex items-center justify-between">
                <span>Discount</span>
                <span className="font-semibold tabular-nums-idr">-{formatIDR(dailyQuery.data.discount)}</span>
              </p>
              <p className="flex items-center justify-between">
                <span>Net</span>
                <span className="font-semibold tabular-nums-idr">{formatIDR(dailyQuery.data.net)}</span>
              </p>
              <p className="flex items-center justify-between">
                <span>Transactions</span>
                <span className="font-semibold">{dailyQuery.data.transactionCount}</span>
              </p>
              <div className="pt-2">
                <p className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Payment Breakdown</p>
                <div className="mt-2 space-y-1">
                  {dailyQuery.data.paymentBreakdown.map((entry) => (
                    <p key={entry.paymentMethod} className="flex items-center justify-between text-sm">
                      <span>{entry.paymentMethod}</span>
                      <span className="font-semibold tabular-nums-idr">{formatIDR(entry.total)}</span>
                    </p>
                  ))}
                </div>
              </div>
            </div>
          ) : null}
        </Card>

        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Top Products</h2>
          <div className="flex flex-wrap items-end gap-2">
            <label className="text-sm text-on-surface-variant">
              From
              <input
                className="mt-1 block h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
                type="date"
                value={from}
                onChange={(event) => setFrom(event.target.value)}
              />
            </label>
            <label className="text-sm text-on-surface-variant">
              To
              <input
                className="mt-1 block h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
                type="date"
                value={to}
                onChange={(event) => setTo(event.target.value)}
              />
            </label>
            <Button variant="secondary" onClick={() => topProductsQuery.refetch()}>
              Refresh
            </Button>
          </div>
          {topProductsQuery.isLoading ? <LoadingBlock title="Loading top products" description="Fetching product performance..." /> : null}
          {topProductsQuery.isError ? (
            <ErrorBlock title="Failed to load top products" description={(topProductsQuery.error as Error).message} onRetry={() => topProductsQuery.refetch()} />
          ) : null}
          {topProductsQuery.isSuccess && topProductsQuery.data.items.length === 0 ? (
            <EmptyBlock title="No top products data" description="Try another date range." />
          ) : null}
          {topProductsQuery.isSuccess && topProductsQuery.data.items.length > 0 ? (
            <div className="space-y-2">
              {topProductsQuery.data.items.map((item) => (
                <div key={item.productId} className="rounded-xl bg-surface-container-low p-3">
                  <p className="font-semibold">{item.name}</p>
                  <p className="text-xs text-on-surface-variant">{item.sku}</p>
                  <p className="text-sm">
                    Qty sold: <span className="font-semibold">{item.qtySold}</span>
                  </p>
                  <p className="text-sm">
                    Revenue: <span className="font-semibold tabular-nums-idr">{formatIDR(item.revenue)}</span>
                  </p>
                </div>
              ))}
            </div>
          ) : null}
        </Card>
      </div>
    </div>
  );
}
