import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/cn";

const badgeVariants = cva(
  "inline-flex items-center rounded-pill px-2 py-0.5 text-[11px] font-mono leading-none whitespace-nowrap",
  {
    variants: {
      variant: {
        default:
          "bg-bg-elevated text-text-primary border border-border-subtle",
        accent:
          "bg-accent-muted text-accent border border-accent/30",
        success:
          "bg-success/10 text-success border border-success/30",
        critical:
          "bg-critical/10 text-critical border border-critical/30",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { badgeVariants };
