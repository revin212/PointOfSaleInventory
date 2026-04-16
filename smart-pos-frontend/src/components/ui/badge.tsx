import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold tracking-wide",
  {
    variants: {
      variant: {
        neutral: "bg-surface-container-low text-on-surface-variant",
        success: "bg-primary/10 text-primary",
        warning: "bg-amber-100 text-amber-700",
        error: "bg-error/10 text-error",
      },
    },
    defaultVariants: {
      variant: "neutral",
    },
  },
);

type BadgeProps = React.HTMLAttributes<HTMLSpanElement> & VariantProps<typeof badgeVariants>;

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}
