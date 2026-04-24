import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { FilterToolbar } from "@/components/shared/filter-toolbar";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { StockBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { createProduct, deleteProduct, listCategories, listProducts, updateProduct, type ProductRow } from "@/features/catalog/catalog-service";
import { formatIDR } from "@/lib/format";
import { ROLE } from "@/types/enums";

const productFormSchema = z.object({
  sku: z.string().min(1, "SKU is required"),
  name: z.string().min(2, "Name is required"),
  categoryId: z.string().min(1, "Category is required"),
  unit: z.string().min(1, "Unit is required"),
  cost: z.number().min(0),
  price: z.number().min(0),
  barcode: z.string().optional(),
  lowStockThreshold: z.number().min(0),
  stock: z.number().min(0),
  active: z.boolean(),
});

type ProductFormValues = z.infer<typeof productFormSchema>;

const defaultValues: ProductFormValues = {
  sku: "",
  name: "",
  categoryId: "",
  unit: "pcs",
  cost: 0,
  price: 0,
  barcode: "",
  lowStockThreshold: 10,
  stock: 0,
  active: true,
};

export function ProductsPage() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const canManage = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const isReadOnly = user?.role === ROLE.CASHIER;

  const [query, setQuery] = useState("");
  const [categoryId, setCategoryId] = useState("ALL");
  const [page, setPage] = useState(0);
  const [editingProduct, setEditingProduct] = useState<ProductRow | null>(null);
  const [deletingProductId, setDeletingProductId] = useState<string | null>(null);

  const categoriesQuery = useQuery({
    queryKey: ["categories-options"],
    queryFn: listCategories,
  });

  const productsQuery = useQuery({
    queryKey: ["products", query, categoryId, page],
    queryFn: () => listProducts({ query, categoryId, page, size: 10 }),
  });

  const form = useForm<ProductFormValues>({
    resolver: zodResolver(productFormSchema),
    defaultValues,
  });

  const createMutation = useMutation({
    mutationFn: (values: ProductFormValues) => {
      const category = categoriesQuery.data?.find((item) => item.id === values.categoryId);
      return createProduct({
        ...values,
        categoryName: category?.name ?? "Unknown",
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      form.reset(defaultValues);
    },
  });

  const updateMutation = useMutation({
    mutationFn: (values: ProductFormValues) => {
      if (!editingProduct) throw new Error("No product selected");
      const category = categoriesQuery.data?.find((item) => item.id === values.categoryId);
      return updateProduct(editingProduct.id, {
        ...values,
        categoryName: category?.name ?? "Unknown",
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      setEditingProduct(null);
      form.reset(defaultValues);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteProduct,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      setDeletingProductId(null);
    },
  });

  const selectedDeleteTarget = useMemo(
    () => productsQuery.data?.content.find((item) => item.id === deletingProductId) ?? null,
    [productsQuery.data?.content, deletingProductId],
  );

  const onSubmit = (values: ProductFormValues) => {
    if (editingProduct) {
      updateMutation.mutate(values);
      return;
    }
    createMutation.mutate(values);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Products"
        subtitle={isReadOnly ? "Read-only mode for cashier role." : "Manage products with category mapping and stock visibility."}
      />

      <FilterToolbar
        searchPlaceholder="Search by name, SKU, barcode"
        searchValue={query}
        onSearchChange={(value) => {
          setPage(0);
          setQuery(value);
        }}
        filters={
          <select
            className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm"
            value={categoryId}
            onChange={(event) => {
              setCategoryId(event.target.value);
              setPage(0);
            }}
          >
            <option value="ALL">All categories</option>
            {categoriesQuery.data?.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        }
      />

      {createMutation.isSuccess || updateMutation.isSuccess || deleteMutation.isSuccess ? (
        <SuccessBlock title="Saved successfully" description="Product data has been updated." />
      ) : null}
      {createMutation.isError || updateMutation.isError || deleteMutation.isError ? (
        <ErrorBlock
          title="Product operation failed"
          description={(createMutation.error as Error)?.message || (updateMutation.error as Error)?.message || (deleteMutation.error as Error)?.message || "Try again."}
        />
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Products List</h2>
          {productsQuery.isLoading ? <LoadingBlock title="Loading products" description="Fetching product rows..." /> : null}
          {productsQuery.isError ? (
            <ErrorBlock title="Failed to load products" description={(productsQuery.error as Error).message} onRetry={() => productsQuery.refetch()} />
          ) : null}
          {productsQuery.isSuccess && productsQuery.data.content.length === 0 ? (
            <EmptyBlock title="No products found" description="Try another query or create a new product." />
          ) : null}
          {productsQuery.isSuccess && productsQuery.data.content.length > 0 ? (
            <div className="space-y-2">
              {productsQuery.data.content.map((product) => (
                <div key={product.id} className="rounded-xl bg-surface-container-low p-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-semibold">{product.name}</p>
                      <p className="text-xs text-on-surface-variant">
                        {product.sku} • {product.categoryName}
                      </p>
                      <p className="text-sm font-semibold tabular-nums-idr">{formatIDR(product.price)}</p>
                    </div>
                    <div className="space-y-2 text-right">
                      <StockBadge stockTone={product.stock <= 0 ? "out" : product.stock <= product.lowStockThreshold ? "low" : "in-stock"} />
                      {canManage ? (
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => {
                              setEditingProduct(product);
                              form.reset({
                                sku: product.sku,
                                name: product.name,
                                categoryId: product.categoryId,
                                unit: product.unit,
                                cost: product.cost,
                                price: product.price,
                                barcode: product.barcode ?? "",
                                lowStockThreshold: product.lowStockThreshold,
                                stock: product.stock,
                                active: product.active,
                              });
                            }}
                          >
                            Edit
                          </Button>
                          <Button variant="destructive" size="sm" onClick={() => setDeletingProductId(product.id)}>
                            Delete
                          </Button>
                        </div>
                      ) : null}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
          {productsQuery.isSuccess ? (
            <div className="flex items-center justify-between rounded-xl bg-surface-container-low p-3 text-sm">
              <p>
                Page {productsQuery.data.page + 1} / {productsQuery.data.totalPages}
              </p>
              <div className="flex gap-2">
                <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => setPage((prev) => Math.max(0, prev - 1))}>
                  Prev
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page + 1 >= productsQuery.data.totalPages}
                  onClick={() => setPage((prev) => prev + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          ) : null}
        </Card>

        <Card className="space-y-3">
          <h2 className="text-lg font-bold">{editingProduct ? "Edit Product" : "Create Product"}</h2>
          {canManage ? (
            <form className="space-y-2" onSubmit={form.handleSubmit(onSubmit)}>
              <div className="space-y-1">
                <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">SKU</label>
                <Input placeholder="e.g. SKU-1001" {...form.register("sku")} />
                <p className="text-xs text-on-surface-variant">Unique code used for search and receipts.</p>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Product name</label>
                <Input placeholder="e.g. Arabica Gayo 250g" {...form.register("name")} />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Category</label>
              <select className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm" {...form.register("categoryId")}>
                <option value="">Select category</option>
                {categoriesQuery.data?.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
              <p className="text-xs text-on-surface-variant">Used for filtering and inventory grouping.</p>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Unit</label>
                  <Input placeholder="e.g. pcs / pack / box" {...form.register("unit")} />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Initial stock</label>
                  <Input type="number" min={0} placeholder="0" {...form.register("stock", { valueAsNumber: true })} />
                  <p className="text-xs text-on-surface-variant">Starting on-hand quantity when creating the product.</p>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Cost (IDR)</label>
                  <Input type="number" min={0} placeholder="e.g. 65000" {...form.register("cost", { valueAsNumber: true })} />
                  <p className="text-xs text-on-surface-variant">Your purchase cost per unit.</p>
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Selling price (IDR)</label>
                  <Input type="number" min={0} placeholder="e.g. 95000" {...form.register("price", { valueAsNumber: true })} />
                  <p className="text-xs text-on-surface-variant">Shown in POS and product list.</p>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Barcode (optional)</label>
                  <Input placeholder="Scan code, if available" {...form.register("barcode")} />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Low stock threshold</label>
                  <Input type="number" min={0} placeholder="e.g. 10" {...form.register("lowStockThreshold", { valueAsNumber: true })} />
                  <p className="text-xs text-on-surface-variant">Mark as low stock when on-hand ≤ threshold.</p>
                </div>
              </div>
              <label className="flex items-center gap-2 text-sm text-on-surface-variant">
                <input type="checkbox" {...form.register("active")} />
                Active
              </label>
              <div className="flex gap-2">
                <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                  {editingProduct ? "Update Product" : "Create Product"}
                </Button>
                {editingProduct ? (
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      setEditingProduct(null);
                      form.reset(defaultValues);
                    }}
                  >
                    Cancel
                  </Button>
                ) : null}
              </div>
            </form>
          ) : (
            <EmptyBlock title="Read-only access" description="Cashier role cannot create or edit products." />
          )}
        </Card>
      </div>

      <ConfirmDialog
        open={Boolean(selectedDeleteTarget)}
        title="Delete this product?"
        description={selectedDeleteTarget ? `This will remove ${selectedDeleteTarget.name}.` : "This action cannot be undone."}
        confirmLabel="Delete"
        loading={deleteMutation.isPending}
        onCancel={() => setDeletingProductId(null)}
        onConfirm={() => {
          if (selectedDeleteTarget) deleteMutation.mutate(selectedDeleteTarget.id);
        }}
      />
    </div>
  );
}
