import type { ReactNode } from "react";

import { Search } from "lucide-react";

import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

type FilterToolbarProps = {
  searchPlaceholder?: string;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  filters?: ReactNode;
  actions?: ReactNode;
  className?: string;
};

export function FilterToolbar({
  searchPlaceholder = "Search...",
  searchValue,
  onSearchChange,
  filters,
  actions,
  className,
}: FilterToolbarProps) {
  return (
    <div className={cn("rounded-xl bg-surface-container-low p-3 sm:p-4", className)}>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
        <div className="relative w-full lg:max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-on-surface-variant" />
          <Input
            className="pl-9"
            value={searchValue}
            onChange={(event) => onSearchChange?.(event.target.value)}
            placeholder={searchPlaceholder}
          />
        </div>
        {filters ? <div className="flex flex-1 flex-wrap items-center gap-2">{filters}</div> : null}
        {actions ? <div className="flex items-center gap-2">{actions}</div> : null}
      </div>
    </div>
  );
}
