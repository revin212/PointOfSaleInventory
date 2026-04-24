import type { ComponentProps } from "react";

import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

export function CurrencyInput({ className, ...props }: ComponentProps<typeof Input>) {
  return (
    <div className={cn("relative", className)}>
      <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs font-bold text-on-surface-variant">
        Rp
      </span>
      <Input className="pl-10 tabular-nums" inputMode="numeric" {...props} />
    </div>
  );
}

