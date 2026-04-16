import type { HTMLAttributes } from "react";

import { cn } from "@/lib/utils";

type AlertProps = HTMLAttributes<HTMLDivElement> & {
  tone?: "default" | "error";
};

export function Alert({ className, tone = "default", ...props }: AlertProps) {
  return (
    <div
      className={cn(
        "rounded-xl border px-4 py-3 text-sm",
        tone === "error" ? "border-error/30 bg-error/10 text-error" : "border-outline-variant/30 bg-surface-container-low text-on-surface",
        className,
      )}
      {...props}
    />
  );
}
