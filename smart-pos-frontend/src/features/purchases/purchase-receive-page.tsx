import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm, useWatch } from "react-hook-form";
import { useParams } from "react-router-dom";

import { PageHeader } from "@/components/shared/page-header";
import { ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { getPurchaseById, receivePurchase } from "@/features/purchases/purchases-service";
import { ROLE } from "@/types/enums";

type ReceiveFormValues = {
  lines: Array<{ productId: string; productName: string; qtyOrdered: number; qtyReceived: number; qtyToReceive: number; cost: number }>;
};

export function PurchaseReceivePage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const canReceive = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const queryClient = useQueryClient();

  const purchaseQuery = useQuery({
    queryKey: ["purchase-detail", id],
    queryFn: () => getPurchaseById(id ?? ""),
    enabled: Boolean(id) && canReceive,
  });

  const form = useForm<ReceiveFormValues>({
    defaultValues: { lines: [] },
  });
  const lines = useWatch({
    control: form.control,
    name: "lines",
  });

  const receiveMutation = useMutation({
    mutationFn: (values: ReceiveFormValues) =>
      receivePurchase({
        purchaseId: id ?? "",
        items: values.lines.map((line) => ({
          productId: line.productId,
          qtyReceived: line.qtyToReceive,
          cost: line.cost,
        })),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["purchases"] });
      await queryClient.invalidateQueries({ queryKey: ["purchase-detail", id] });
    },
  });

  const initialized = useMemo(() => {
    if (!purchaseQuery.data) return false;
    const current = form.getValues("lines");
    if (current.length > 0) return true;
    form.reset({
      lines: purchaseQuery.data.items.map((item) => ({
        productId: item.productId,
        productName: item.productName,
        qtyOrdered: item.qtyOrdered,
        qtyReceived: item.qtyReceived,
        qtyToReceive: 0,
        cost: item.cost,
      })),
    });
    return true;
  }, [purchaseQuery.data, form]);

  if (!canReceive) {
    return <ErrorBlock title="Access denied" description="Receive flow is only available for Owner and Warehouse roles." />;
  }
  if (!id) {
    return <ErrorBlock title="Purchase ID missing" description="Open this page from purchase list." />;
  }
  if (purchaseQuery.isLoading || !initialized) {
    return <LoadingBlock title="Loading purchase detail" description="Preparing receive form..." />;
  }
  if (purchaseQuery.isError || !purchaseQuery.data) {
    return <ErrorBlock title="Failed to load purchase" description={(purchaseQuery.error as Error)?.message || "Try again."} onRetry={() => purchaseQuery.refetch()} />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Receive ${purchaseQuery.data.purchaseNo}`}
        subtitle={`${purchaseQuery.data.supplierName} • Status ${purchaseQuery.data.status}`}
      />

      {receiveMutation.isSuccess ? <SuccessBlock title="Receive completed" description="Purchase status has been updated." /> : null}
      {receiveMutation.isError ? (
        <ErrorBlock title="Receive failed" description={(receiveMutation.error as Error).message || "Try again."} onRetry={() => receiveMutation.reset()} retryLabel="Reset" />
      ) : null}

      <Card className="space-y-3">
        <h2 className="text-lg font-bold">Receive quantities</h2>
        <form
          className="space-y-2"
          onSubmit={form.handleSubmit((values) => {
            const hasInvalid = values.lines.some((line) => line.qtyToReceive < 0 || line.qtyToReceive > line.qtyOrdered - line.qtyReceived);
            if (hasInvalid) {
              receiveMutation.reset();
              return;
            }
            receiveMutation.mutate(values);
          })}
        >
          {lines.map((line, index) => {
            const remaining = line.qtyOrdered - line.qtyReceived;
            return (
              <div key={line.productId} className="rounded-xl bg-surface-container-low p-3">
                <p className="font-semibold">{line.productName}</p>
                <p className="text-xs text-on-surface-variant">
                  Ordered {line.qtyOrdered} • Received {line.qtyReceived} • Remaining {remaining}
                </p>
                <div className="mt-2 grid grid-cols-2 gap-2">
                  <Input
                    type="number"
                    min={0}
                    max={remaining}
                    placeholder="Qty to receive"
                    {...form.register(`lines.${index}.qtyToReceive`, { valueAsNumber: true })}
                  />
                  <Input type="number" min={0} placeholder="Cost" {...form.register(`lines.${index}.cost`, { valueAsNumber: true })} />
                </div>
              </div>
            );
          })}
          <Button type="submit" disabled={receiveMutation.isPending}>
            {receiveMutation.isPending ? "Submitting..." : "Submit Receive"}
          </Button>
        </form>
      </Card>
    </div>
  );
}
