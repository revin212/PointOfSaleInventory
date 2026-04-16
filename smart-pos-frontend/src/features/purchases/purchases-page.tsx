import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Link } from "react-router-dom";

import { FilterToolbar } from "@/components/shared/filter-toolbar";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { createPurchase, listPurchases, purchaseStatusOptions } from "@/features/purchases/purchases-service";
import { MOCK_PRODUCTS } from "@/mocks/commerce";
import { ROLE } from "@/types/enums";
import type { PurchaseStatus } from "@/types/purchase";

const createPurchaseSchema = z.object({
  supplierName: z.string().min(2, "Supplier name is required"),
  productId: z.string().min(1, "Product is required"),
  qtyOrdered: z.number().min(1, "Qty must be at least 1"),
  cost: z.number().min(0, "Cost must be positive"),
});

type CreatePurchaseValues = z.infer<typeof createPurchaseSchema>;

const defaultValues: CreatePurchaseValues = {
  supplierName: "",
  productId: "",
  qtyOrdered: 1,
  cost: 0,
};

export function PurchasesPage() {
  const { user } = useAuth();
  const canManage = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const queryClient = useQueryClient();

  const [status, setStatus] = useState<PurchaseStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);

  const purchasesQuery = useQuery({
    queryKey: ["purchases", status, page],
    queryFn: () => listPurchases({ status, page, size: 10 }),
    enabled: canManage,
  });

  const form = useForm<CreatePurchaseValues>({
    resolver: zodResolver(createPurchaseSchema),
    defaultValues,
  });

  const createMutation = useMutation({
    mutationFn: (values: CreatePurchaseValues) =>
      createPurchase({
        supplierName: values.supplierName,
        items: [{ productId: values.productId, qtyOrdered: values.qtyOrdered, cost: values.cost }],
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["purchases"] });
      form.reset(defaultValues);
    },
  });

  if (!canManage) {
    return <EmptyBlock title="Access denied" description="Purchases module is available only for Owner and Warehouse roles." />;
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Purchases" subtitle="Create purchase orders and continue to receive flow." />

      <FilterToolbar
        filters={
          <select
            className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as PurchaseStatus | "ALL");
              setPage(0);
            }}
          >
            {purchaseStatusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        }
      />

      {createMutation.isSuccess ? <SuccessBlock title="Purchase created" description="Purchase order saved and ready for receiving." /> : null}
      {createMutation.isError ? (
        <ErrorBlock title="Create purchase failed" description={(createMutation.error as Error).message || "Try again."} onRetry={() => createMutation.reset()} retryLabel="Reset" />
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Purchase orders</h2>
          {purchasesQuery.isLoading ? <LoadingBlock title="Loading purchases" description="Fetching purchase order data..." /> : null}
          {purchasesQuery.isError ? (
            <ErrorBlock title="Failed to load purchases" description={(purchasesQuery.error as Error).message} onRetry={() => purchasesQuery.refetch()} />
          ) : null}
          {purchasesQuery.isSuccess && purchasesQuery.data.content.length === 0 ? (
            <EmptyBlock title="No purchase orders" description="Create a new purchase from the form." />
          ) : null}
          {purchasesQuery.isSuccess && purchasesQuery.data.content.length > 0 ? (
            <div className="space-y-2">
              {purchasesQuery.data.content.map((purchase) => (
                <div key={purchase.id} className="rounded-xl bg-surface-container-low p-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-semibold">{purchase.purchaseNo}</p>
                      <p className="text-xs text-on-surface-variant">
                        {purchase.supplierName} • {new Date(purchase.createdAt).toLocaleString("id-ID")}
                      </p>
                    </div>
                    <StatusBadge
                      tone={purchase.status === "RECEIVED" ? "success" : purchase.status === "PARTIALLY_RECEIVED" ? "warning" : "neutral"}
                      label={purchase.status}
                    />
                  </div>
                  <div className="mt-3 flex justify-end">
                    <Link
                      to={`/purchases/${purchase.id}/receive`}
                      className="inline-flex h-9 items-center justify-center rounded-lg bg-surface-container-highest px-3 text-xs font-semibold text-on-surface transition hover:opacity-90"
                    >
                      Receive
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          ) : null}

          {purchasesQuery.isSuccess ? (
            <div className="flex items-center justify-between rounded-xl bg-surface-container-low p-3 text-sm">
              <p>
                Page {purchasesQuery.data.page + 1} / {purchasesQuery.data.totalPages}
              </p>
              <div className="flex gap-2">
                <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => setPage((prev) => Math.max(0, prev - 1))}>
                  Prev
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page + 1 >= purchasesQuery.data.totalPages}
                  onClick={() => setPage((prev) => prev + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          ) : null}
        </Card>

        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Create purchase order</h2>
          <form className="space-y-2" onSubmit={form.handleSubmit((values) => createMutation.mutate(values))}>
            <Input placeholder="Supplier name" {...form.register("supplierName")} />
            <select className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm" {...form.register("productId")}>
              <option value="">Select product</option>
              {MOCK_PRODUCTS.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} ({product.sku})
                </option>
              ))}
            </select>
            <div className="grid grid-cols-2 gap-2">
              <Input type="number" placeholder="Qty ordered" {...form.register("qtyOrdered", { valueAsNumber: true })} />
              <Input type="number" placeholder="Cost" {...form.register("cost", { valueAsNumber: true })} />
            </div>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? "Saving..." : "Create Purchase"}
            </Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
