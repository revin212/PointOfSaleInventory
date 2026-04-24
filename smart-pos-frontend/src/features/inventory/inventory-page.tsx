import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { FilterToolbar } from "@/components/shared/filter-toolbar";
import { Modal } from "@/components/shared/modal";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { StockBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { listProducts } from "@/features/catalog/catalog-service";
import { createStockAdjustment, listOnHand } from "@/features/inventory/inventory-service";
import { ROLE } from "@/types/enums";

const adjustmentSchema = z.object({
  productId: z.string().min(1, "Product is required"),
  qtyDelta: z.number(),
  reason: z.string().min(4, "Reason is required"),
});

type AdjustmentValues = z.infer<typeof adjustmentSchema>;

const defaultAdjustmentValues: AdjustmentValues = {
  productId: "",
  qtyDelta: 0,
  reason: "",
};

export function InventoryPage() {
  const { user } = useAuth();
  const canAccess = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const canManage = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [lowOnly, setLowOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false);

  const stockQuery = useQuery({
    queryKey: ["stock-on-hand", query, lowOnly, page],
    queryFn: () => listOnHand({ query, lowOnly, page, size: 10 }),
    enabled: canAccess,
  });

  const productsQuery = useQuery({
    queryKey: ["inventory-adjust-products"],
    queryFn: () => listProducts({ size: 200 }),
    enabled: canManage,
  });

  const productOptions = useMemo(
    () =>
      (productsQuery.data?.content ?? []).map((p) => ({
        id: p.id,
        label: `${p.name} (${p.sku})`,
      })),
    [productsQuery.data?.content],
  );

  const form = useForm<AdjustmentValues>({
    resolver: zodResolver(adjustmentSchema),
    defaultValues: defaultAdjustmentValues,
  });

  const adjustmentMutation = useMutation({
    mutationFn: createStockAdjustment,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["stock-on-hand"] });
      await queryClient.invalidateQueries({ queryKey: ["stock-movements"] });
      form.reset(defaultAdjustmentValues);
      setIsAdjustModalOpen(false);
    },
  });

  if (!canAccess) {
    return <EmptyBlock title="Access denied" description="Inventory page is only available for Owner and Warehouse roles." />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Inventory On-Hand"
        subtitle="Track stock levels and quickly identify low-stock items."
        actions={
          <Button variant="secondary" onClick={() => setIsAdjustModalOpen(true)}>
            Stock adjustment
          </Button>
        }
      />
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

      {adjustmentMutation.isSuccess ? <SuccessBlock title="Adjustment saved" description="Stock movement has been recorded." /> : null}
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

      <Modal
        open={isAdjustModalOpen}
        title="Stock adjustment"
        onClose={() => {
          setIsAdjustModalOpen(false);
          form.reset(defaultAdjustmentValues);
          adjustmentMutation.reset();
        }}
      >
        <form className="space-y-3" onSubmit={form.handleSubmit((values) => adjustmentMutation.mutate(values))}>
          {adjustmentMutation.isError ? (
            <ErrorBlock
              title="Adjustment failed"
              description={(adjustmentMutation.error as Error).message || "Try again."}
              onRetry={() => adjustmentMutation.reset()}
              retryLabel="Reset"
            />
          ) : null}

          <div className="space-y-1">
            <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Product</label>
            <select className="h-10 w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm" {...form.register("productId")}>
              <option value="">Select product</option>
              {productOptions.map((opt) => (
                <option key={opt.id} value={opt.id}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-1">
              <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Qty delta</label>
              <Input type="number" placeholder="e.g. -2 or 10" {...form.register("qtyDelta", { valueAsNumber: true })} />
              <p className="text-xs text-on-surface-variant">Use negative to reduce stock, positive to add.</p>
            </div>
            <div className="space-y-1">
              <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Reason</label>
              <Input placeholder="e.g. damaged goods" {...form.register("reason")} />
            </div>
          </div>

          <div className="flex justify-end gap-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                setIsAdjustModalOpen(false);
                form.reset(defaultAdjustmentValues);
                adjustmentMutation.reset();
              }}
              disabled={adjustmentMutation.isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={adjustmentMutation.isPending}>
              {adjustmentMutation.isPending ? "Saving..." : "Submit adjustment"}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
