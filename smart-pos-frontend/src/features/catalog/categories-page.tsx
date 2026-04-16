import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { createCategory, deleteCategory, listCategories, updateCategory, type Category } from "@/features/catalog/catalog-service";
import { ROLE } from "@/types/enums";

const formSchema = z.object({
  name: z.string().min(2, "Name is required"),
  active: z.boolean(),
});

type FormValues = z.infer<typeof formSchema>;
const defaults: FormValues = { name: "", active: true };

export function CategoriesPage() {
  const { user } = useAuth();
  const canManage = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Category | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const categoriesQuery = useQuery({
    queryKey: ["categories"],
    queryFn: listCategories,
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: defaults,
  });

  const createMutation = useMutation({
    mutationFn: createCategory,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["categories"] });
      form.reset(defaults);
    },
  });
  const updateMutation = useMutation({
    mutationFn: (values: FormValues) => {
      if (!editing) throw new Error("No category selected");
      return updateCategory(editing.id, values);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["categories"] });
      setEditing(null);
      form.reset(defaults);
    },
  });
  const deleteMutation = useMutation({
    mutationFn: deleteCategory,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["categories"] });
      setDeletingId(null);
    },
  });

  const onSubmit = (values: FormValues) => {
    if (editing) {
      updateMutation.mutate(values);
      return;
    }
    createMutation.mutate(values);
  };

  const deleteTarget = categoriesQuery.data?.find((item) => item.id === deletingId) ?? null;

  return (
    <div className="space-y-6">
      <PageHeader title="Categories" subtitle={canManage ? "Manage category master data." : "No access for this role."} />

      {createMutation.isSuccess || updateMutation.isSuccess || deleteMutation.isSuccess ? (
        <SuccessBlock title="Saved successfully" description="Category data has been updated." />
      ) : null}
      {createMutation.isError || updateMutation.isError || deleteMutation.isError ? (
        <ErrorBlock
          title="Category operation failed"
          description={(createMutation.error as Error)?.message || (updateMutation.error as Error)?.message || (deleteMutation.error as Error)?.message || "Try again."}
        />
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Category list</h2>
          {categoriesQuery.isLoading ? <LoadingBlock title="Loading categories" description="Fetching category data..." /> : null}
          {categoriesQuery.isError ? (
            <ErrorBlock title="Failed to load categories" description={(categoriesQuery.error as Error).message} onRetry={() => categoriesQuery.refetch()} />
          ) : null}
          {categoriesQuery.isSuccess && categoriesQuery.data.length === 0 ? (
            <EmptyBlock title="No categories" description="Create your first category." />
          ) : null}
          {categoriesQuery.isSuccess && categoriesQuery.data.length > 0 ? (
            <div className="space-y-2">
              {categoriesQuery.data.map((category) => (
                <div key={category.id} className="flex items-center justify-between rounded-xl bg-surface-container-low p-3">
                  <div>
                    <p className="font-semibold">{category.name}</p>
                    <p className="text-xs text-on-surface-variant">{category.active ? "Active" : "Inactive"}</p>
                  </div>
                  {canManage ? (
                    <div className="flex gap-2">
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => {
                          setEditing(category);
                          form.reset({ name: category.name, active: category.active });
                        }}
                      >
                        Edit
                      </Button>
                      <Button variant="destructive" size="sm" onClick={() => setDeletingId(category.id)}>
                        Delete
                      </Button>
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          ) : null}
        </Card>

        <Card className="space-y-3">
          <h2 className="text-lg font-bold">{editing ? "Edit Category" : "Create Category"}</h2>
          {canManage ? (
            <form className="space-y-2" onSubmit={form.handleSubmit(onSubmit)}>
              <Input placeholder="Category name" {...form.register("name")} />
              <label className="flex items-center gap-2 text-sm text-on-surface-variant">
                <input type="checkbox" {...form.register("active")} />
                Active
              </label>
              <div className="flex gap-2">
                <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                  {editing ? "Update Category" : "Create Category"}
                </Button>
                {editing ? (
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      setEditing(null);
                      form.reset(defaults);
                    }}
                  >
                    Cancel
                  </Button>
                ) : null}
              </div>
            </form>
          ) : (
            <EmptyBlock title="Access denied" description="Only Owner and Warehouse can manage categories." />
          )}
        </Card>
      </div>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete this category?"
        description={deleteTarget ? `This will remove ${deleteTarget.name}.` : "This action cannot be undone."}
        confirmLabel="Delete"
        loading={deleteMutation.isPending}
        onCancel={() => setDeletingId(null)}
        onConfirm={() => {
          if (deleteTarget) deleteMutation.mutate(deleteTarget.id);
        }}
      />
    </div>
  );
}
