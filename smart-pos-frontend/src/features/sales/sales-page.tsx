import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { FilterToolbar } from "@/components/shared/filter-toolbar";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock } from "@/components/shared/state-blocks";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { formatIDR } from "@/lib/format";
import { getSales, paymentMethodOptions } from "@/features/sales/sales-service";
import type { PaymentMethod } from "@/types/enums";

export function SalesPage() {
  const [query, setQuery] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod | "ALL">("ALL");
  const [page, setPage] = useState(0);
  const size = 10;

  const salesQuery = useQuery({
    queryKey: ["sales", query, paymentMethod, page, size],
    queryFn: () => getSales({ query, paymentMethod, page, size }),
  });

  return (
    <div className="space-y-4 md:space-y-6">
      <PageHeader title="Sales History" subtitle="Paginated sales list with payment filter and role-safe actions." />
      <FilterToolbar
        searchPlaceholder="Search invoice, cashier, or SKU"
        searchValue={query}
        onSearchChange={(value) => {
          setPage(0);
          setQuery(value);
        }}
        filters={
          <select
            className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
            value={paymentMethod}
            onChange={(event) => {
              setPage(0);
              setPaymentMethod(event.target.value as PaymentMethod | "ALL");
            }}
          >
            {paymentMethodOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        }
      />

      {salesQuery.isLoading ? <LoadingBlock title="Loading sales" description="Fetching sales history from server..." /> : null}
      {salesQuery.isError ? (
        <ErrorBlock
          title="Failed to load sales"
          description={(salesQuery.error as Error).message || "Unexpected error occurred."}
          onRetry={() => salesQuery.refetch()}
        />
      ) : null}
      {salesQuery.isSuccess && salesQuery.data.content.length === 0 ? (
        <EmptyBlock title="No sales found" description="Try changing filters or date range." />
      ) : null}

      {salesQuery.isSuccess && salesQuery.data.content.length > 0 ? (
        <>
          <div className="space-y-2 md:hidden">
            {salesQuery.data.content.map((sale) => (
              <Card key={sale.id} className="space-y-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate font-semibold">{sale.invoiceNo}</p>
                    <p className="text-xs text-on-surface-variant">{new Date(sale.createdAt).toLocaleString("id-ID")}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="font-semibold tabular-nums-idr">{formatIDR(sale.totals.total)}</p>
                    <div className="mt-1 flex justify-end">
                      <StatusBadge tone={sale.status === "COMPLETED" ? "success" : "error"} label={sale.status} />
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs text-on-surface-variant">
                  <div className="rounded-xl bg-surface-container-low px-3 py-2">
                    <p className="font-semibold text-on-surface">Cashier</p>
                    <p className="truncate">{sale.cashierName}</p>
                  </div>
                  <div className="rounded-xl bg-surface-container-low px-3 py-2">
                    <p className="font-semibold text-on-surface">Payment</p>
                    <p className="truncate">{sale.paymentMethod}</p>
                  </div>
                </div>

                <Link
                  to={`/sales/${sale.id}`}
                  className="inline-flex h-10 w-full items-center justify-center rounded-xl bg-surface-container-low px-4 text-sm font-semibold text-on-surface transition hover:bg-surface-container-highest"
                >
                  View details
                </Link>
              </Card>
            ))}
          </div>

          <Card className="hidden overflow-x-auto p-0 md:block">
            <table className="w-full text-left">
              <thead className="bg-surface-container-low text-xs uppercase tracking-widest text-on-surface-variant">
                <tr>
                  <th className="px-4 py-3">Invoice</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Cashier</th>
                  <th className="px-4 py-3">Payment</th>
                  <th className="px-4 py-3 text-right">Total</th>
                  <th className="px-4 py-3 text-center">Status</th>
                  <th className="px-4 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody>
                {salesQuery.data.content.map((sale) => (
                  <tr key={sale.id} className="border-t border-outline-variant/20">
                    <td className="px-4 py-3 font-semibold">{sale.invoiceNo}</td>
                    <td className="px-4 py-3 text-sm">{new Date(sale.createdAt).toLocaleString("id-ID")}</td>
                    <td className="px-4 py-3 text-sm">{sale.cashierName}</td>
                    <td className="px-4 py-3 text-sm">{sale.paymentMethod}</td>
                    <td className="px-4 py-3 text-right font-semibold tabular-nums-idr">{formatIDR(sale.totals.total)}</td>
                    <td className="px-4 py-3 text-center">
                      <StatusBadge tone={sale.status === "COMPLETED" ? "success" : "error"} label={sale.status} />
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Link
                        to={`/sales/${sale.id}`}
                        className="inline-flex h-9 items-center justify-center rounded-lg bg-surface-container-low px-3 text-xs font-semibold text-on-surface transition hover:bg-surface-container-highest"
                      >
                        View Details
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </>
      ) : null}

      {salesQuery.isSuccess ? (
        <div className="flex items-center justify-between rounded-xl bg-surface-container-low p-3 text-sm">
          <p>
            Page {salesQuery.data.page + 1} of {salesQuery.data.totalPages} ({salesQuery.data.totalElements} items)
          </p>
          <div className="flex gap-2">
            <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => setPage((prev) => Math.max(0, prev - 1))}>
              Previous
            </Button>
            <Button
              variant="secondary"
              size="sm"
              disabled={page + 1 >= salesQuery.data.totalPages}
              onClick={() => setPage((prev) => prev + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
