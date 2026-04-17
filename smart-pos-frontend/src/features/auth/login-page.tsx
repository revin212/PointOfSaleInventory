import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate } from "react-router-dom";

import { AppLogo } from "@/components/shared/app-logo";
import { ErrorBlock } from "@/components/shared/state-blocks";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { USE_MOCKS } from "@/lib/env";
import { ApiError } from "@/lib/api-error";
import { ROLE, type Role } from "@/types/enums";

const loginSchema = z.object({
  email: z.string().email("Please enter a valid email address."),
  password: z.string().min(6, "Password must be at least 6 characters."),
  role: z.enum([ROLE.OWNER, ROLE.CASHIER, ROLE.WAREHOUSE]),
});

type LoginFormValues = z.infer<typeof loginSchema>;

const roleLanding: Record<Role, string> = {
  OWNER: "/dashboard",
  CASHIER: "/dashboard",
  WAREHOUSE: "/dashboard",
};

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: USE_MOCKS
      ? { email: "owner@store.com", password: "password123", role: ROLE.OWNER }
      : { email: "owner@smartpos.local", password: "Password123!", role: ROLE.OWNER },
  });

  const roleOptions = useMemo(
    () => [
      { value: ROLE.OWNER, label: "Owner" },
      { value: ROLE.CASHIER, label: "Cashier" },
      { value: ROLE.WAREHOUSE, label: "Warehouse" },
    ],
    [],
  );

  const onSubmit = async (values: LoginFormValues) => {
    setErrorMessage(null);
    try {
      if (USE_MOCKS) {
        if (!values.email.endsWith("@store.com") || values.password !== "password123") {
          setErrorMessage("Use an @store.com email and password123 for mock mode.");
          return;
        }
        const user = await login(values.email, values.password, values.role);
        navigate(roleLanding[user.role], { replace: true });
        return;
      }
      const user = await login(values.email, values.password);
      navigate(roleLanding[user.role], { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message || "Invalid email or password.");
      } else if (error instanceof Error) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("Unexpected error. Please try again.");
      }
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-surface p-6">
      <div className="absolute inset-0 -z-10 overflow-hidden">
        <div className="absolute -left-16 -top-16 h-56 w-56 rounded-full bg-primary/10 blur-3xl" />
        <div className="absolute -bottom-20 -right-16 h-72 w-72 rounded-full bg-surface-container-highest/60 blur-3xl" />
      </div>
      <div className="w-full max-w-md space-y-6">
        <div className="flex justify-center">
          <AppLogo />
        </div>
        <Card className="shadow-ambient">
          <CardHeader>
            <CardTitle className="text-2xl font-black">Sign in to POS</CardTitle>
            <CardDescription>
              {USE_MOCKS
                ? "Mock mode: pick any role to explore the UI."
                : "Sign in with the credentials provisioned by your owner account."}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
              <div className="space-y-1.5">
                <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant" htmlFor="email">
                  Email
                </label>
                <Input id="email" type="email" placeholder="owner@smartpos.local" {...register("email")} />
                {errors.email ? <p className="text-xs text-error">{errors.email.message}</p> : null}
              </div>
              <div className="space-y-1.5">
                <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant" htmlFor="password">
                  Password
                </label>
                <Input id="password" type="password" placeholder="******" {...register("password")} />
                {errors.password ? <p className="text-xs text-error">{errors.password.message}</p> : null}
              </div>
              <div className="space-y-1.5">
                <label className="text-xs font-bold uppercase tracking-widest text-on-surface-variant" htmlFor="role">
                  Role {USE_MOCKS ? "" : "(ignored — role comes from backend)"}
                </label>
                <select
                  id="role"
                  className="h-10 w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                  {...register("role")}
                  disabled={!USE_MOCKS}
                >
                  {roleOptions.map((role) => (
                    <option key={role.value} value={role.value}>
                      {role.label}
                    </option>
                  ))}
                </select>
                {errors.role ? <p className="text-xs text-error">{errors.role.message}</p> : null}
              </div>
              <Button type="submit" className="w-full" disabled={isSubmitting}>
                {isSubmitting ? "Signing in..." : "Sign In"}
              </Button>
            </form>
          </CardContent>
        </Card>
        {errorMessage ? (
          <ErrorBlock
            title="Sign-in failed"
            description={errorMessage}
          />
        ) : null}
      </div>
    </div>
  );
}
