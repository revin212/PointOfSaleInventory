import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { PageHeader } from "@/components/shared/page-header";
import { EmptyBlock, ErrorBlock, LoadingBlock, SuccessBlock } from "@/components/shared/state-blocks";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { createUser, listUsers, setUserActive, updateUser, type UserRecord } from "@/features/users/users-service";
import { ROLE, type Role } from "@/types/enums";

const schema = z.object({
  name: z.string().min(2, "Name is required"),
  email: z.string().email("Valid email is required"),
  role: z.enum([ROLE.OWNER, ROLE.CASHIER, ROLE.WAREHOUSE]),
  active: z.boolean(),
  password: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;
const defaults: FormValues = { name: "", email: "", role: ROLE.CASHIER, active: true, password: "" };

const roleOptions: Array<{ value: Role; label: string }> = [
  { value: ROLE.OWNER, label: "Owner" },
  { value: ROLE.CASHIER, label: "Cashier" },
  { value: ROLE.WAREHOUSE, label: "Warehouse" },
];

export function UsersPage() {
  const { user } = useAuth();
  const isOwner = user?.role === ROLE.OWNER;
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<UserRecord | null>(null);

  const usersQuery = useQuery({
    queryKey: ["users"],
    queryFn: listUsers,
    enabled: isOwner,
  });

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  });

  const createMutation = useMutation({
    mutationFn: (values: FormValues) => {
      if (!values.password || values.password.length < 8) {
        throw new Error("Password must be at least 8 characters for new users.");
      }
      return createUser(values);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["users"] });
      form.reset(defaults);
    },
  });
  const updateMutation = useMutation({
    mutationFn: (values: FormValues) => {
      if (!editing) throw new Error("No user selected.");
      return updateUser(editing.id, values);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["users"] });
      setEditing(null);
      form.reset(defaults);
    },
  });
  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => setUserActive(id, active),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });

  if (!isOwner) {
    return <EmptyBlock title="Access denied" description="Users & Roles is owner-only." />;
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Users & Roles" subtitle="Owner-only user management and role assignment." />

      {createMutation.isSuccess || updateMutation.isSuccess || activeMutation.isSuccess ? (
        <SuccessBlock title="Saved successfully" description="User data has been updated." />
      ) : null}
      {createMutation.isError || updateMutation.isError || activeMutation.isError ? (
        <ErrorBlock
          title="User operation failed"
          description={(createMutation.error as Error)?.message || (updateMutation.error as Error)?.message || (activeMutation.error as Error)?.message || "Try again."}
        />
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <Card className="space-y-3">
          <h2 className="text-lg font-bold">User list</h2>
          {usersQuery.isLoading ? <LoadingBlock title="Loading users" description="Fetching user list..." /> : null}
          {usersQuery.isError ? (
            <ErrorBlock title="Failed to load users" description={(usersQuery.error as Error).message} onRetry={() => usersQuery.refetch()} />
          ) : null}
          {usersQuery.isSuccess && usersQuery.data.length === 0 ? (
            <EmptyBlock title="No users found" description="Create a user from the form." />
          ) : null}
          {usersQuery.isSuccess && usersQuery.data.length > 0 ? (
            <div className="space-y-2">
              {usersQuery.data.map((row) => (
                <div key={row.id} className="rounded-xl bg-surface-container-low p-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-semibold">{row.name}</p>
                      <p className="text-xs text-on-surface-variant">{row.email}</p>
                      <p className="text-xs font-semibold">{row.role}</p>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => {
                          setEditing(row);
                          form.reset({ name: row.name, email: row.email, role: row.role, active: row.active, password: "" });
                        }}
                      >
                        Edit
                      </Button>
                      <Button
                        variant={row.active ? "destructive" : "secondary"}
                        size="sm"
                        onClick={() => activeMutation.mutate({ id: row.id, active: !row.active })}
                        disabled={activeMutation.isPending}
                      >
                        {row.active ? "Deactivate" : "Activate"}
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </Card>

        <Card className="space-y-3">
          <h2 className="text-lg font-bold">{editing ? "Edit User" : "Create User"}</h2>
          <form className="space-y-2" onSubmit={form.handleSubmit((values) => (editing ? updateMutation.mutate(values) : createMutation.mutate(values)))}>
            <Input placeholder="Name" {...form.register("name")} />
            <Input placeholder="Email" {...form.register("email")} />
            <Input
              type="password"
              placeholder={editing ? "New password (leave blank to keep)" : "Password (min 8 chars)"}
              {...form.register("password")}
            />
            <select className="h-10 rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm" {...form.register("role")}>
              {roleOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <label className="flex items-center gap-2 text-sm text-on-surface-variant">
              <input type="checkbox" {...form.register("active")} />
              Active
            </label>
            <div className="flex gap-2">
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {editing ? "Update User" : "Create User"}
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
        </Card>
      </div>
    </div>
  );
}
