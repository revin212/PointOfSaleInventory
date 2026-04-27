import { useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";

import { FilterToolbar } from "@/components/shared/filter-toolbar";
import { CurrencyInput } from "@/components/shared/currency-input";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { StockBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { getAppConfig } from "@/features/app-config/app-config-api";
import { listProducts, type ProductRow } from "@/features/catalog/catalog-service";
import { listPaymentTypes } from "@/features/payment-types/payment-types-api";
import { createSale } from "@/features/sales/sales-service";
import { formatIDR } from "@/lib/format";
import { PAYMENT_METHOD, type PaymentMethod } from "@/types/enums";

type CartLine = {
  productId: string;
  sku: string;
  name: string;
  qty: number;
  unitPrice: number;
  lineDiscount: number;
};

function roundIdr(amount: number) {
  return Math.round(amount);
}

export function PosPage() {
  const [query, setQuery] = useState("");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [discount, setDiscount] = useState(0);
  const [paymentTypeId, setPaymentTypeId] = useState<string>("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>(PAYMENT_METHOD.CASH);
  const [paidAmount, setPaidAmount] = useState(0);

  const productsQuery = useQuery({
    queryKey: ["pos-products", query],
    queryFn: () => listProducts({ query, page: 0, size: 50 }),
  });

  const appConfigQuery = useQuery({
    queryKey: ["app-config"],
    queryFn: getAppConfig,
  });

  const paymentTypesQuery = useQuery({
    queryKey: ["payment-types"],
    queryFn: listPaymentTypes,
  });

  const products = useMemo<ProductRow[]>(() => productsQuery.data?.content ?? [], [productsQuery.data]);
  const paymentTypes = useMemo(() => paymentTypesQuery.data ?? [], [paymentTypesQuery.data]);
  const selectedPaymentType = useMemo(
    () => paymentTypes.find((pt) => pt.id === paymentTypeId) ?? null,
    [paymentTypes, paymentTypeId],
  );

  const subtotal = useMemo(() => cart.reduce((sum, item) => sum + item.unitPrice * item.qty, 0), [cart]);
  const lineDiscountTotal = useMemo(() => cart.reduce((sum, item) => sum + item.lineDiscount, 0), [cart]);
  const net = Math.max(0, subtotal - lineDiscountTotal - discount);
  const vatRate = appConfigQuery.data?.taxEnabled ? Number(appConfigQuery.data.vatRate ?? 0) : 0;
  const taxAmount = useMemo(() => (vatRate > 0 ? roundIdr(net * vatRate) : 0), [net, vatRate]);
  const adminFee = selectedPaymentType ? Number(selectedPaymentType.adminFee ?? 0) : 0;
  const total = Math.max(0, net + taxAmount + adminFee);
  const change = Math.max(0, paidAmount - total);

  const checkoutMutation = useMutation({
    mutationFn: async () => {
      if (!cart.length) {
        throw new Error("Cart is empty. Add at least one item.");
      }
      if (paidAmount < total) {
        throw new Error("Paid amount is lower than total.");
      }
      return createSale({
        items: cart.map((line) => ({
          productId: line.productId,
          qty: line.qty,
          unitPrice: line.unitPrice,
          lineDiscount: line.lineDiscount,
        })),
        discount,
        paymentMethod: selectedPaymentType?.method ?? paymentMethod,
        paymentTypeId: selectedPaymentType?.id,
        paidAmount,
      });
    },
    onSuccess: () => {
      setCart([]);
      setDiscount(0);
      setPaidAmount(0);
      setPaymentTypeId("");
    },
  });

  const addToCart = (product: ProductRow) => {
    setCart((prev) => {
      const existing = prev.find((item) => item.productId === product.id);
      if (existing) {
        return prev.map((item) => (item.productId === product.id ? { ...item, qty: item.qty + 1 } : item));
      }
      return [
        ...prev,
        {
          productId: product.id,
          sku: product.sku,
          name: product.name,
          qty: 1,
          unitPrice: product.price,
          lineDiscount: 0,
        },
      ];
    });
  };

  return (
    <div className="space-y-4 md:space-y-6">
      <PageHeader title="POS" subtitle="Create sale with enum-safe payment methods and quick cart actions." />

      <FilterToolbar searchPlaceholder="Search by product or SKU" searchValue={query} onSearchChange={setQuery} />

      {checkoutMutation.isSuccess ? (
        <SuccessBlock
          title="Sale completed"
          description={`Invoice ${checkoutMutation.data.invoiceNo} paid via ${selectedPaymentType?.name ?? paymentMethod}.`}
        />
      ) : null}

      <div className="grid gap-4 md:gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <Card className="space-y-4">
          <h2 className="text-lg font-bold">Product list</h2>
          {productsQuery.isLoading ? <LoadingBlock title="Loading products" description="Fetching product catalog..." /> : null}
          {productsQuery.isError ? (
            <ErrorBlock
              title="Failed to load products"
              description={(productsQuery.error as Error).message}
              onRetry={() => productsQuery.refetch()}
            />
          ) : null}
          {productsQuery.isSuccess && products.length === 0 ? (
            <EmptyBlock title="No products found" description="Try a different keyword or SKU." />
          ) : null}
          {productsQuery.isSuccess && products.length > 0 ? (
            <div className="space-y-2">
              {products.map((product) => (
                <div key={product.id} className="flex items-start justify-between gap-3 rounded-xl bg-surface-container-low p-3">
                  <div className="min-w-0">
                    <p className="font-semibold">{product.name}</p>
                    <p className="text-xs text-on-surface-variant">{product.sku}</p>
                    <p className="text-sm font-bold tabular-nums-idr">{formatIDR(product.price)}</p>
                  </div>
                  <div className="flex shrink-0 flex-col items-end gap-2 sm:flex-row sm:items-center sm:gap-3">
                    <StockBadge
                      stockTone={
                        product.stock <= 0 ? "out" : product.stock <= product.lowStockThreshold ? "low" : "in-stock"
                      }
                      stock={product.stock}
                      unit={product.unit}
                    />
                    <Button size="sm" className="w-full sm:w-auto" onClick={() => addToCart(product)}>
                      <Plus className="h-4 w-4" />
                      Add
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </Card>

        <Card className="space-y-4">
          <h2 className="text-lg font-bold">Cart</h2>
          {!cart.length ? (
            <EmptyBlock title="Cart is empty" description="Add products from the list to start checkout." />
          ) : (
            <div className="space-y-2">
              {cart.map((line) => (
                <div key={line.productId} className="rounded-xl bg-surface-container-low p-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="font-semibold">{line.name}</p>
                      <p className="text-xs text-on-surface-variant">{line.sku}</p>
                    </div>
                    <button
                      className="rounded-lg p-2 text-error hover:bg-error/10"
                      type="button"
                      onClick={() => setCart((prev) => prev.filter((item) => item.productId !== line.productId))}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                  <div className="mt-3 grid grid-cols-2 gap-2">
                    <div className="space-y-1">
                      <label
                        className="text-xs font-bold uppercase tracking-widest text-on-surface-variant"
                        htmlFor={`qty-${line.productId}`}
                      >
                        Qty
                      </label>
                      <Input
                        id={`qty-${line.productId}`}
                        type="number"
                        min={1}
                        value={line.qty}
                        onChange={(event) =>
                          setCart((prev) =>
                            prev.map((item) =>
                              item.productId === line.productId ? { ...item, qty: Math.max(1, Number(event.target.value || 1)) } : item,
                            ),
                          )
                        }
                      />
                    </div>
                    <div className="space-y-1">
                      <label
                        className="text-xs font-bold uppercase tracking-widest text-on-surface-variant"
                        htmlFor={`lineDiscount-${line.productId}`}
                      >
                        Line discount (Rp)
                      </label>
                      <CurrencyInput
                        id={`lineDiscount-${line.productId}`}
                        type="number"
                        min={0}
                        value={line.lineDiscount}
                        onChange={(event) =>
                          setCart((prev) =>
                            prev.map((item) =>
                              item.productId === line.productId
                                ? { ...item, lineDiscount: Math.max(0, Number(event.target.value || 0)) }
                                : item,
                            ),
                          )
                        }
                        placeholder="0"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="space-y-2 rounded-xl bg-surface-container-low p-3">
            <div className="flex items-center justify-between text-sm">
              <span>Subtotal</span>
              <span className="tabular-nums-idr">{formatIDR(subtotal)}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span>Discount</span>
              <span className="tabular-nums-idr">-{formatIDR(lineDiscountTotal + discount)}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span>Tax{vatRate > 0 ? ` (${Math.round(vatRate * 100)}%)` : ""}</span>
              <span className="tabular-nums-idr">{formatIDR(taxAmount)}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span>Admin fee</span>
              <span className="tabular-nums-idr">{formatIDR(adminFee)}</span>
            </div>
            <div className="flex items-center justify-between text-base font-bold">
              <span>Total</span>
              <span className="tabular-nums-idr">{formatIDR(total)}</span>
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant" htmlFor="paymentType">
              Payment Type
            </label>
            <select
              id="paymentType"
              className="h-10 w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
              value={paymentTypeId}
              onChange={(event) => {
                const next = event.target.value;
                setPaymentTypeId(next);
                const found = paymentTypes.find((pt) => pt.id === next);
                if (found) setPaymentMethod(found.method);
              }}
            >
              <option value="">Select payment type</option>
              {paymentTypes.map((pt) => (
                <option key={pt.id} value={pt.id}>
                  {pt.name} {pt.adminFee > 0 ? `• +${formatIDR(pt.adminFee)}` : ""}
                </option>
              ))}
            </select>
            {!paymentTypes.length ? (
              <p className="text-xs text-on-surface-variant">No payment types loaded yet. Make sure backend is updated and seeded.</p>
            ) : null}
          </div>

          <div className="grid gap-2 sm:grid-cols-2">
            <div className="space-y-1">
              <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant" htmlFor="orderDiscount">
                Order discount (Rp)
              </label>
              <CurrencyInput
                id="orderDiscount"
                type="number"
                value={discount}
                onChange={(event) => setDiscount(Math.max(0, Number(event.target.value || 0)))}
                placeholder="0"
                min={0}
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant" htmlFor="paidAmount">
                Paid amount (Rp)
              </label>
              <CurrencyInput
                id="paidAmount"
                type="number"
                value={paidAmount}
                onChange={(event) => setPaidAmount(Math.max(0, Number(event.target.value || 0)))}
                placeholder="0"
                min={0}
              />
            </div>
          </div>

          <p className="text-sm text-on-surface-variant">Change: <span className="font-semibold tabular-nums-idr">{formatIDR(change)}</span></p>

          {checkoutMutation.isError ? (
            <ErrorBlock title="Checkout failed" description={(checkoutMutation.error as Error).message} onRetry={() => checkoutMutation.reset()} retryLabel="Reset" />
          ) : null}

          <Button className="w-full" onClick={() => checkoutMutation.mutate()} disabled={checkoutMutation.isPending}>
            {checkoutMutation.isPending ? "Processing..." : "Complete Sale"}
          </Button>
        </Card>
      </div>
    </div>
  );
}
