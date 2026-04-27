import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useAuth } from "@/features/auth/auth-context";
import { cancelSaleById, getSaleById } from "@/features/sales/sales-service";
import { formatIDR } from "@/lib/format";
import { ROLE } from "@/types/enums";

export function SaleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [openCancelDialog, setOpenCancelDialog] = useState(false);

  const saleQuery = useQuery({
    queryKey: ["sale-detail", id],
    queryFn: () => getSaleById(id ?? ""),
    enabled: Boolean(id),
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelSaleById(id ?? ""),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["sales"] });
      await queryClient.invalidateQueries({ queryKey: ["sale-detail", id] });
      setOpenCancelDialog(false);
    },
  });

  if (!id) {
    return <EmptyBlock title="Sale ID missing" description="Open sale detail from sales history table." />;
  }

  if (saleQuery.isLoading) {
    return <LoadingBlock title="Loading sale detail" description="Fetching transaction summary and line items..." />;
  }

  if (saleQuery.isError) {
    return <ErrorBlock title="Failed to load sale detail" description={(saleQuery.error as Error).message} onRetry={() => saleQuery.refetch()} />;
  }

  const sale = saleQuery.data;
  const canCancel = user?.role === ROLE.OWNER && sale.status !== "CANCELLED";

  return (
    <div className="space-y-4 md:space-y-6">
      <PageHeader
        title={`Sale ${sale.invoiceNo}`}
        subtitle={`Cashier ${sale.cashierName} • ${new Date(sale.createdAt).toLocaleString("id-ID")}`}
        actions={
          <div className="flex w-full flex-wrap items-center justify-between gap-2 sm:w-auto sm:justify-start">
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                // Use browser history when possible; otherwise fallback to sales list.
                if (window.history.length > 1) navigate(-1);
                else navigate("/sales");
              }}
            >
              <ArrowLeft className="h-4 w-4" />
              Back
            </Button>
            {canCancel ? (
              <Button className="sm:ml-4" variant="destructive" onClick={() => setOpenCancelDialog(true)}>
                Cancel Sale
              </Button>
            ) : null}
          </div>
        }
      />

      {cancelMutation.isSuccess ? (
        <SuccessBlock title="Sale cancelled" description="Status updated to CANCELLED and stock reversal should be handled by backend." />
      ) : null}
      {cancelMutation.isError ? (
        <ErrorBlock title="Cancel failed" description={(cancelMutation.error as Error).message} onRetry={() => cancelMutation.reset()} retryLabel="Reset" />
      ) : null}

      <div className="grid gap-4 md:gap-6 lg:grid-cols-[1fr_0.85fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Items</h2>
          <div className="space-y-2">
            {sale.items.map((item) => (
              <div key={`${item.productId}-${item.sku}`} className="rounded-xl bg-surface-container-low p-3">
                <p className="font-semibold">{item.name}</p>
                <p className="text-xs text-on-surface-variant">{item.sku}</p>
                <div className="mt-1 flex items-center justify-between text-sm">
                  <span>
                    {item.qty} x {formatIDR(item.unitPrice)}
                  </span>
                  <span className="font-semibold tabular-nums-idr">{formatIDR(item.qty * item.unitPrice - (item.lineDiscount ?? 0))}</span>
                </div>
              </div>
            ))}
          </div>
        </Card>
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Summary</h2>
          <div className="space-y-2 text-sm">
            <p className="flex items-center justify-between">
              <span>Status</span>
              <StatusBadge tone={sale.status === "COMPLETED" ? "success" : "error"} label={sale.status} />
            </p>
            <p className="flex items-center justify-between">
              <span>Payment Method</span>
              <span className="font-semibold">{sale.paymentMethod}</span>
            </p>
            <p className="flex items-center justify-between">
              <span>Subtotal</span>
              <span className="tabular-nums-idr">{formatIDR(sale.totals.subtotal)}</span>
            </p>
            <p className="flex items-center justify-between">
              <span>Discount</span>
              <span className="tabular-nums-idr">-{formatIDR(sale.totals.discount)}</span>
            </p>
            {sale.totals.taxAmount != null ? (
              <p className="flex items-center justify-between">
                <span>Tax{sale.totals.taxRate != null ? ` (${Math.round(sale.totals.taxRate * 100)}%)` : ""}</span>
                <span className="tabular-nums-idr">{formatIDR(sale.totals.taxAmount)}</span>
              </p>
            ) : null}
            {sale.totals.adminFee != null ? (
              <p className="flex items-center justify-between">
                <span>Admin fee</span>
                <span className="tabular-nums-idr">{formatIDR(sale.totals.adminFee)}</span>
              </p>
            ) : null}
            <p className="flex items-center justify-between text-base font-bold">
              <span>Total</span>
              <span className="tabular-nums-idr">{formatIDR(sale.totals.total)}</span>
            </p>
            <p className="flex items-center justify-between">
              <span>Paid</span>
              <span className="tabular-nums-idr">{formatIDR(sale.totals.paidAmount)}</span>
            </p>
            <p className="flex items-center justify-between">
              <span>Change</span>
              <span className="tabular-nums-idr">{formatIDR(sale.totals.changeAmount)}</span>
            </p>
          </div>
        </Card>
      </div>

      <ConfirmDialog
        open={openCancelDialog}
        title="Cancel this sale?"
        description="This action is owner-only and should trigger backend stock reversal."
        confirmLabel="Yes, cancel sale"
        loading={cancelMutation.isPending}
        onCancel={() => setOpenCancelDialog(false)}
        onConfirm={() => cancelMutation.mutate()}
      />
    </div>
  );
}
