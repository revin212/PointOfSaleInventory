import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { listPaymentTypesManage, updatePaymentType, type PaymentType } from "@/features/payment-types/payment-types-api";
import { PAYMENT_METHOD, type PaymentMethod } from "@/types/enums";
import { formatIDR } from "@/lib/format";

const paymentMethodOptions: Array<{ value: PaymentMethod; label: string }> = [
  { value: PAYMENT_METHOD.CASH, label: "Cash" },
  { value: PAYMENT_METHOD.TRANSFER, label: "Transfer" },
  { value: PAYMENT_METHOD.EWALLET, label: "E-Wallet" },
];

export function PaymentTypesPage() {
  const queryClient = useQueryClient();
  const [drafts, setDrafts] = useState<Record<string, { name: string; adminFee: number; active: boolean }>>({});

  const paymentTypesQuery = useQuery({
    queryKey: ["payment-types", "manage"],
    queryFn: listPaymentTypesManage,
  });

  const paymentTypes = useMemo(() => paymentTypesQuery.data ?? [], [paymentTypesQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async (pt: PaymentType) => {
      const draft = drafts[pt.id] ?? { name: pt.name, adminFee: pt.adminFee, active: pt.active };
      return updatePaymentType(pt.id, {
        method: pt.method,
        name: draft.name,
        adminFee: Number(draft.adminFee ?? 0),
        active: Boolean(draft.active),
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["payment-types"] });
    },
  });

  return (
    <div className="space-y-4 md:space-y-6">
      <PageHeader title="Payment Types" subtitle="Manage payment methods and their admin fee (flat IDR)." />

      {saveMutation.isSuccess ? <SuccessBlock title="Saved" description="Payment type updated." /> : null}
      {saveMutation.isError ? (
        <ErrorBlock
          title="Save failed"
          description={(saveMutation.error as Error).message || "Try again."}
          onRetry={() => saveMutation.reset()}
          retryLabel="Reset"
        />
      ) : null}

      <Card className="space-y-3">
        <h2 className="text-lg font-bold">Configured payment types</h2>

        {paymentTypesQuery.isLoading ? <LoadingBlock title="Loading payment types" description="Fetching payment configuration..." /> : null}
        {paymentTypesQuery.isError ? (
          <ErrorBlock title="Failed to load" description={(paymentTypesQuery.error as Error).message} onRetry={() => paymentTypesQuery.refetch()} />
        ) : null}
        {paymentTypesQuery.isSuccess && paymentTypes.length === 0 ? (
          <EmptyBlock title="No payment types" description="Seeder should create default types. You can also upsert via API." />
        ) : null}

        {paymentTypesQuery.isSuccess && paymentTypes.length > 0 ? (
          <div className="space-y-2">
            {paymentMethodOptions.map((methodOpt) => {
              const pt = paymentTypes.find((x) => x.method === methodOpt.value);
              if (!pt) return null;
              const draft = drafts[pt.id] ?? { name: pt.name, adminFee: pt.adminFee, active: pt.active };

              return (
                <div key={pt.id} className="rounded-xl bg-surface-container-low p-3">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-semibold">{methodOpt.label}</p>
                      <p className="text-xs text-on-surface-variant">Method: {pt.method}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <label className="inline-flex items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={draft.active}
                          onChange={(e) =>
                            setDrafts((prev) => ({
                              ...prev,
                              [pt.id]: { ...draft, active: e.target.checked },
                            }))
                          }
                        />
                        Active
                      </label>
                      <Button variant="secondary" size="sm" onClick={() => saveMutation.mutate(pt)} disabled={saveMutation.isPending}>
                        {saveMutation.isPending ? "Saving..." : "Save"}
                      </Button>
                    </div>
                  </div>

                  <div className="mt-3 grid gap-2 sm:grid-cols-2">
                    <div className="space-y-1">
                      <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Display name</label>
                      <Input
                        value={draft.name}
                        onChange={(e) =>
                          setDrafts((prev) => ({
                            ...prev,
                            [pt.id]: { ...draft, name: e.target.value },
                          }))
                        }
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Admin fee (IDR)</label>
                      <Input
                        type="number"
                        min={0}
                        value={draft.adminFee}
                        onChange={(e) =>
                          setDrafts((prev) => ({
                            ...prev,
                            [pt.id]: { ...draft, adminFee: Math.max(0, Number(e.target.value || 0)) },
                          }))
                        }
                      />
                      <p className="text-xs text-on-surface-variant">Current: {formatIDR(pt.adminFee)}</p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : null}
      </Card>
    </div>
  );
}

