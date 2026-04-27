import { Badge } from "@/components/ui/badge";

type StatusTone = "success" | "warning" | "error" | "neutral";
type StockTone = "in-stock" | "low" | "out";

const toneMap: Record<StatusTone, "success" | "warning" | "error" | "neutral"> = {
  success: "success",
  warning: "warning",
  error: "error",
  neutral: "neutral",
};

const stockMap: Record<StockTone, { text: string; variant: "success" | "warning" | "error" }> = {
  "in-stock": { text: "In stock", variant: "success" },
  low: { text: "Low stock", variant: "warning" },
  out: { text: "Out of stock", variant: "error" },
};

type StatusBadgeProps = {
  tone: StatusTone;
  label: string;
};

export function StatusBadge({ tone, label }: StatusBadgeProps) {
  return <Badge variant={toneMap[tone]}>{label}</Badge>;
}

type StockBadgeProps = {
  stockTone: StockTone;
  stock?: number;
  unit?: string;
};

export function StockBadge({ stockTone, stock, unit }: StockBadgeProps) {
  const config = stockMap[stockTone];
  return (
    <Badge variant={config.variant} className="inline-flex items-center gap-1.5 whitespace-nowrap">
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      <span>{config.text}</span>
      {typeof stock === "number" ? (
        <span className="text-xs opacity-80">
          • {stock}
          {unit ? ` ${unit}` : ""}
        </span>
      ) : null}
    </Badge>
  );
}
