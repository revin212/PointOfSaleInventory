import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { FilterToolbar } from "@/components/shared/filter-toolbar";
import { Modal } from "@/components/shared/modal";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { listProducts } from "@/features/catalog/catalog-service";
import { createStockAdjustment, listMovements, type MovementType } from "@/features/inventory/inventory-service";
import { ROLE } from "@/types/enums";

const adjustmentSchema = z.object({
  productId: z.string().min(1, "Product is required"),
  qtyDelta: z.number(),
  reason: z.string().min(4, "Reason is required"),
});

type AdjustmentValues = z.infer<typeof adjustmentSchema>;

const defaultValues: AdjustmentValues = {
  productId: "",
  qtyDelta: 0,
  reason: "",
};

export function StockAdjustmentsPage() {
  const { user } = useAuth();
  const canManage = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const queryClient = useQueryClient();
  const [productFilter, setProductFilter] = useState("ALL");
  const [movementType, setMovementType] = useState<MovementType | "ALL">("ADJUSTMENT");
  const [historyOpen, setHistoryOpen] = useState(false);

  const movementsQuery = useQuery({
    queryKey: ["stock-movements", productFilter, movementType],
    queryFn: () =>
      listMovements({
        productId: productFilter,
        type: movementType === "ALL" ? undefined : movementType,
        page: 0,
        size: 20,
      }),
    enabled: canManage,
  });

  const productsQuery = useQuery({
    queryKey: ["stock-products"],
    queryFn: () => listProducts({ size: 200 }),
    enabled: canManage,
  });

  const form = useForm<AdjustmentValues>({
    resolver: zodResolver(adjustmentSchema),
    defaultValues,
  });

  const adjustmentMutation = useMutation({
    mutationFn: createStockAdjustment,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["stock-movements"] });
      await queryClient.invalidateQueries({ queryKey: ["stock-on-hand"] });
      form.reset(defaultValues);
    },
  });

  if (!canManage) {
    return <EmptyBlock title="Access denied" description="Stock adjustment is available only for Owner and Warehouse roles." />;
  }

  const products = (productsQuery.data?.content ?? []).map((product) => ({
    productId: product.id,
    name: product.name,
    sku: product.sku,
  }));

  return (
    <div className="space-y-4 md:space-y-6">
      <PageHeader
        title="Stock Adjustments"
        subtitle="Create stock delta adjustments with mandatory reason."
        actions={
          <Button variant="secondary" type="button" onClick={() => setHistoryOpen(true)}>
            View History
          </Button>
        }
      />

      {adjustmentMutation.isSuccess ? <SuccessBlock title="Adjustment saved" description="Stock movement has been recorded." /> : null}
      {adjustmentMutation.isError ? (
        <ErrorBlock title="Adjustment failed" description={(adjustmentMutation.error as Error).message || "Try again."} onRetry={() => adjustmentMutation.reset()} retryLabel="Reset" />
      ) : null}

      <Modal open={historyOpen} title="Stock Adjustment History" onClose={() => setHistoryOpen(false)}>
        <div className="space-y-3">
          <FilterToolbar
            filters={
              <div className="flex flex-wrap items-center gap-2">
                <select
                  className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
                  value={productFilter}
                  onChange={(event) => setProductFilter(event.target.value)}
                >
                  <option value="ALL">All products</option>
                  {products.map((product) => (
                    <option key={product.productId} value={product.productId}>
                      {product.name}
                    </option>
                  ))}
                </select>
                <select
                  className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
                  value={movementType}
                  onChange={(event) => setMovementType(event.target.value as MovementType | "ALL")}
                  aria-label="Movement type"
                >
                  <option value="ADJUSTMENT">Adjustments only</option>
                  <option value="ALL">All movement types</option>
                  <option value="PURCHASE_RECEIVE">Purchase receive</option>
                  <option value="SALE">Sale</option>
                  <option value="SALE_CANCEL">Sale cancel</option>
                </select>
              </div>
            }
          />

          {movementsQuery.isLoading ? <LoadingBlock title="Loading movements" description="Fetching stock movement rows..." /> : null}
          {movementsQuery.isError ? (
            <ErrorBlock title="Failed to load movements" description={(movementsQuery.error as Error).message} onRetry={() => movementsQuery.refetch()} />
          ) : null}
          {movementsQuery.isSuccess && movementsQuery.data.content.length === 0 ? (
            <EmptyBlock title="No movements found" description="Try another product filter." />
          ) : null}
          {movementsQuery.isSuccess && movementsQuery.data.content.length > 0 ? (
            <div className="space-y-2">
              {movementsQuery.data.content.map((movement) => (
                <div key={movement.id} className="rounded-xl bg-surface-container-low p-3">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="font-semibold">{movement.productName}</p>
                      <p className="text-xs text-on-surface-variant">
                        {movement.type} • {movement.reason}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className={`text-sm font-bold ${movement.qtyDelta < 0 ? "text-error" : "text-primary"}`}>
                        {movement.qtyDelta > 0 ? `+${movement.qtyDelta}` : movement.qtyDelta}
                      </p>
                      <p className="text-xs text-on-surface-variant">{new Date(movement.createdAt).toLocaleString("id-ID")}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : null}

          <div className="flex items-center justify-end gap-2 pt-2">
            <Button variant="secondary" type="button" onClick={() => setHistoryOpen(false)}>
              Close
            </Button>
          </div>
        </div>
      </Modal>

      <div className="grid gap-4 md:gap-6 lg:grid-cols-[0.95fr_1.05fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Create adjustment</h2>
          <form className="space-y-2" onSubmit={form.handleSubmit((values) => adjustmentMutation.mutate(values))}>
            <select className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm" {...form.register("productId")}>
              <option value="">Select product</option>
              {products.map((product) => (
                <option key={product.productId} value={product.productId}>
                  {product.name} ({product.sku})
                </option>
              ))}
            </select>
            <Input
              type="number"
              placeholder="Qty delta (negative or positive)"
              {...form.register("qtyDelta", { valueAsNumber: true })}
            />
            <Input placeholder="Reason" {...form.register("reason")} />
            <Button type="submit" disabled={adjustmentMutation.isPending}>
              {adjustmentMutation.isPending ? "Saving..." : "Submit Adjustment"}
            </Button>
          </form>
        </Card>

        <Card className="space-y-3">
          <h2 className="text-lg font-bold">History</h2>
          <p className="text-sm text-on-surface-variant">
            View adjustment and movement history with filters in a focused popup.
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <Button type="button" variant="secondary" onClick={() => setHistoryOpen(true)}>
              View History
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => movementsQuery.refetch()}
              disabled={!movementsQuery.isSuccess && !movementsQuery.isError}
            >
              Refresh
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
