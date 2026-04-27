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
import { createSupplier, deleteSupplier, listSuppliers, updateSupplier, type Supplier } from "@/features/catalog/catalog-service";
import { ROLE } from "@/types/enums";

const formSchema = z.object({
  name: z.string().min(2, "Name is required"),
  phone: z.string().min(8, "Phone is required"),
  contactPerson: z.string().min(2, "Contact person is required"),
  active: z.boolean(),
});

type FormValues = z.infer<typeof formSchema>;
const defaults: FormValues = { name: "", phone: "", contactPerson: "", active: true };

export function SuppliersPage() {
  const { user } = useAuth();
  const canManage = user?.role === ROLE.OWNER || user?.role === ROLE.WAREHOUSE;
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Supplier | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const suppliersQuery = useQuery({
    queryKey: ["suppliers"],
    queryFn: listSuppliers,
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: defaults,
  });

  const createMutation = useMutation({
    mutationFn: createSupplier,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["suppliers"] });
      form.reset(defaults);
    },
  });
  const updateMutation = useMutation({
    mutationFn: (values: FormValues) => {
      if (!editing) throw new Error("No supplier selected");
      return updateSupplier(editing.id, values);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["suppliers"] });
      setEditing(null);
      form.reset(defaults);
    },
  });
  const deleteMutation = useMutation({
    mutationFn: deleteSupplier,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["suppliers"] });
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

  const deleteTarget = suppliersQuery.data?.find((item) => item.id === deletingId) ?? null;

  return (
    <div className="space-y-4 md:space-y-6">
      <PageHeader title="Suppliers" subtitle={canManage ? "Maintain supplier records and contacts." : "No access for this role."} />

      {createMutation.isSuccess || updateMutation.isSuccess || deleteMutation.isSuccess ? (
        <SuccessBlock title="Saved successfully" description="Supplier data has been updated." />
      ) : null}
      {createMutation.isError || updateMutation.isError || deleteMutation.isError ? (
        <ErrorBlock
          title="Supplier operation failed"
          description={(createMutation.error as Error)?.message || (updateMutation.error as Error)?.message || (deleteMutation.error as Error)?.message || "Try again."}
        />
      ) : null}

      <div className="grid gap-4 md:gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">Supplier list</h2>
          {suppliersQuery.isLoading ? <LoadingBlock title="Loading suppliers" description="Fetching supplier data..." /> : null}
          {suppliersQuery.isError ? (
            <ErrorBlock title="Failed to load suppliers" description={(suppliersQuery.error as Error).message} onRetry={() => suppliersQuery.refetch()} />
          ) : null}
          {suppliersQuery.isSuccess && suppliersQuery.data.length === 0 ? (
            <EmptyBlock title="No suppliers" description="Create your first supplier record." />
          ) : null}
          {suppliersQuery.isSuccess && suppliersQuery.data.length > 0 ? (
            <div className="space-y-2">
              {suppliersQuery.data.map((supplier) => (
                <div key={supplier.id} className="flex items-center justify-between rounded-xl bg-surface-container-low p-3">
                  <div>
                    <p className="font-semibold">{supplier.name}</p>
                    <p className="text-xs text-on-surface-variant">
                      {supplier.contactPerson} • {supplier.phone}
                    </p>
                  </div>
                  {canManage ? (
                    <div className="flex gap-2">
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => {
                          setEditing(supplier);
                          form.reset({
                            name: supplier.name,
                            phone: supplier.phone,
                            contactPerson: supplier.contactPerson,
                            active: supplier.active,
                          });
                        }}
                      >
                        Edit
                      </Button>
                      <Button variant="destructive" size="sm" onClick={() => setDeletingId(supplier.id)}>
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
          <h2 className="text-lg font-bold">{editing ? "Edit Supplier" : "Create Supplier"}</h2>
          {canManage ? (
            <form className="space-y-2" onSubmit={form.handleSubmit(onSubmit)}>
              <Input placeholder="Supplier name" {...form.register("name")} />
              <Input placeholder="Phone number" {...form.register("phone")} />
              <Input placeholder="Contact person" {...form.register("contactPerson")} />
              <label className="flex items-center gap-2 text-sm text-on-surface-variant">
                <input type="checkbox" {...form.register("active")} />
                Active
              </label>
              <div className="flex gap-2">
                <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                  {editing ? "Update Supplier" : "Create Supplier"}
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
            <EmptyBlock title="Access denied" description="Only Owner and Warehouse can manage suppliers." />
          )}
        </Card>
      </div>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete this supplier?"
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
