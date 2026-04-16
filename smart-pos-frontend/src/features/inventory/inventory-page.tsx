import { useState } from "react";
import { useQuery } from "@tanstack/react-query";

import { FilterToolbar } from "@/components/shared/filter-toolbar";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "@/components/shared/state-blocks";
import { StockBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";
import { listOnHand } from "@/features/inventory/inventory-service";
import { ROLE } from "@/types/enums";

export function InventoryPage() {
  const { user } = useAuth();
  const canAccess = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const [query, setQuery] = useState("");
  const [lowOnly, setLowOnly] = useState(false);
  const [page, setPage] = useState(0);

  const stockQuery = useQuery({
    queryKey: ["stock-on-hand", query, lowOnly, page],
    queryFn: () => listOnHand({ query, lowOnly, page, size: 10 }),
    enabled: canAccess,
  });

  if (!canAccess) {
    return <EmptyBlock title="Access denied" description="Inventory page is only available for Owner and Warehouse roles." />;
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Inventory On-Hand" subtitle="Track stock levels and quickly identify low-stock items." />
      <FilterToolbar
        searchPlaceholder="Search by name or SKU"
        searchValue={query}
        onSearchChange={(value) => {
          setQuery(value);
          setPage(0);
        }}
        actions={
          <label className="inline-flex items-center gap-2 rounded-xl bg-surface-container-low px-3 py-2 text-sm text-on-surface-variant">
            <input
              type="checkbox"
              checked={lowOnly}
              onChange={(event) => {
                setLowOnly(event.target.checked);
                setPage(0);
              }}
            />
            Low stock only
          </label>
        }
      />

      {stockQuery.isLoading ? <LoadingBlock title="Loading stock data" description="Fetching inventory on-hand rows..." /> : null}
      {stockQuery.isError ? (
        <ErrorBlock title="Failed to load inventory" description={(stockQuery.error as Error).message} onRetry={() => stockQuery.refetch()} />
      ) : null}
      {stockQuery.isSuccess && stockQuery.data.content.length === 0 ? (
        <EmptyBlock title="No stock rows found" description="Try clearing search or low-stock filter." />
      ) : null}

      {stockQuery.isSuccess && stockQuery.data.content.length > 0 ? (
        <Card className="space-y-2">
          {stockQuery.data.content.map((row) => (
            <div key={row.productId} className="flex items-center justify-between rounded-xl bg-surface-container-low p-3">
              <div>
                <p className="font-semibold">{row.name}</p>
                <p className="text-xs text-on-surface-variant">
                  {row.sku} • Threshold {row.lowStockThreshold} {row.unit}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-bold">
                  {row.stock} {row.unit}
                </p>
                <StockBadge stockTone={row.stock <= 0 ? "out" : row.stock <= row.lowStockThreshold ? "low" : "in-stock"} />
              </div>
            </div>
          ))}

          <div className="flex items-center justify-between rounded-xl bg-surface-container-low p-3 text-sm">
            <p>
              Page {stockQuery.data.page + 1} / {stockQuery.data.totalPages}
            </p>
            <div className="flex gap-2">
              <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => setPage((prev) => Math.max(0, prev - 1))}>
                Prev
              </Button>
              <Button
                variant="secondary"
                size="sm"
                disabled={page + 1 >= stockQuery.data.totalPages}
                onClick={() => setPage((prev) => prev + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </Card>
      ) : null}
    </div>
  );
}
