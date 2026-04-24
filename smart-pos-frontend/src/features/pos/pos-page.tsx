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
import { listProducts, type ProductRow } from "@/features/catalog/catalog-service";
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

const paymentOptions: Array<{ value: PaymentMethod; label: string }> = [
  { value: PAYMENT_METHOD.CASH, label: "Cash" },
  { value: PAYMENT_METHOD.TRANSFER, label: "Transfer" },
  { value: PAYMENT_METHOD.EWALLET, label: "E-Wallet" },
];

export function PosPage() {
  const [query, setQuery] = useState("");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [discount, setDiscount] = useState(0);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>(PAYMENT_METHOD.CASH);
  const [paidAmount, setPaidAmount] = useState(0);

  const productsQuery = useQuery({
    queryKey: ["pos-products", query],
    queryFn: () => listProducts({ query, page: 0, size: 50 }),
  });

  const products = useMemo<ProductRow[]>(() => productsQuery.data?.content ?? [], [productsQuery.data]);

  const subtotal = useMemo(() => cart.reduce((sum, item) => sum + item.unitPrice * item.qty, 0), [cart]);
  const lineDiscountTotal = useMemo(() => cart.reduce((sum, item) => sum + item.lineDiscount, 0), [cart]);
  const total = Math.max(0, subtotal - lineDiscountTotal - discount);
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
        paymentMethod,
        paidAmount,
      });
    },
    onSuccess: () => {
      setCart([]);
      setDiscount(0);
      setPaidAmount(0);
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
    <div className="space-y-6">
      <PageHeader title="POS" subtitle="Create sale with enum-safe payment methods and quick cart actions." />

      <FilterToolbar searchPlaceholder="Search by product or SKU" searchValue={query} onSearchChange={setQuery} />

      {checkoutMutation.isSuccess ? (
        <SuccessBlock
          title="Sale completed"
          description={`Invoice ${checkoutMutation.data.invoiceNo} paid via ${paymentMethod}.`}
        />
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
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
                <div key={product.id} className="flex items-center justify-between rounded-xl bg-surface-container-low p-3">
                  <div>
                    <p className="font-semibold">{product.name}</p>
                    <p className="text-xs text-on-surface-variant">{product.sku}</p>
                    <p className="text-sm font-bold tabular-nums-idr">{formatIDR(product.price)}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <StockBadge stockTone={product.stock <= 0 ? "out" : product.stock <= 10 ? "low" : "in-stock"} />
                    <Button size="sm" onClick={() => addToCart(product)}>
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
                  <div className="mt-2 flex items-center gap-2">
                    <Input
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
                    <Input
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
                      placeholder="Line discount"
                    />
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
            <div className="flex items-center justify-between text-base font-bold">
              <span>Total</span>
              <span className="tabular-nums-idr">{formatIDR(total)}</span>
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant" htmlFor="paymentMethod">
              Payment Method
            </label>
            <select
              id="paymentMethod"
              className="h-10 w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
              value={paymentMethod}
              onChange={(event) => setPaymentMethod(event.target.value as PaymentMethod)}
            >
              {paymentOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <CurrencyInput
              type="number"
              value={discount}
              onChange={(event) => setDiscount(Math.max(0, Number(event.target.value || 0)))}
              placeholder="Order discount"
              min={0}
            />
            <CurrencyInput
              type="number"
              value={paidAmount}
              onChange={(event) => setPaidAmount(Math.max(0, Number(event.target.value || 0)))}
              placeholder="Paid amount"
              min={0}
            />
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
