import type { LucideIcon } from "lucide-react";
import { ArrowUpRight } from "lucide-react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type KpiCardProps = {
  label: string;
  value: string;
  trend?: string;
  icon?: LucideIcon;
};

export function KpiCard({ label, value, trend, icon: Icon }: KpiCardProps) {
  return (
    <Card>
      <CardHeader className="mb-2 flex flex-row items-center justify-between space-y-0">
        <CardDescription className="text-[11px] font-bold uppercase tracking-widest">{label}</CardDescription>
        {Icon ? <Icon className="h-4 w-4 text-primary" /> : null}
      </CardHeader>
      <CardContent className="space-y-1">
        <CardTitle className="text-xl tabular-nums-idr sm:text-2xl">{value}</CardTitle>
        {trend ? (
          <p className="inline-flex items-center gap-1 text-xs font-semibold text-primary">
            <ArrowUpRight className="h-3.5 w-3.5" />
            {trend}
          </p>
        ) : null}
      </CardContent>
    </Card>
  );
}
