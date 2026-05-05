"use client";

import { useEffect, useState } from "react";
import { animate, useMotionValue, useMotionValueEvent } from "framer-motion";
import { ArrowUpRight, ArrowDownRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import { easeOutQuint, slow } from "@/lib/motion";
import { formatCurrencyEur, formatInteger } from "@/lib/format";
import { cn } from "@/lib/cn";

type MetricFormat = "integer" | "currency-eur";

interface MetricCardProps {
  label: string;
  value: number;
  format: MetricFormat;
  delta?: { value: number; direction: "up" | "down" };
  subtitle?: string;
}

function formatValue(value: number, format: MetricFormat): string {
  return format === "currency-eur"
    ? formatCurrencyEur(value)
    : formatInteger(value);
}

export function MetricCard({ label, value, format, delta, subtitle }: MetricCardProps) {
  const motionValue = useMotionValue(0);
  const [display, setDisplay] = useState<string>(formatValue(0, format));

  useMotionValueEvent(motionValue, "change", (latest) => {
    setDisplay(formatValue(latest, format));
  });

  useEffect(() => {
    const controls = animate(motionValue, value, {
      duration: slow,
      ease: easeOutQuint,
    });
    return () => controls.stop();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const DeltaIcon = delta?.direction === "down" ? ArrowDownRight : ArrowUpRight;

  return (
    <Card className="p-5 flex flex-col gap-2">
      <span className="text-[11px] uppercase tracking-wider text-text-tertiary font-sans">
        {label}
      </span>
      <span className="font-mono text-[40px] font-medium tracking-tight text-text-primary leading-none tabular-nums">
        {display}
      </span>
      {delta && (
        <span
          className={cn(
            "inline-flex items-center gap-1 text-[12px] font-mono",
            delta.direction === "up" ? "text-accent" : "text-critical",
          )}
        >
          <DeltaIcon size={12} strokeWidth={2.25} />
          {delta.value}%
        </span>
      )}
      {subtitle && (
        <span className="text-xs text-text-tertiary mt-1">{subtitle}</span>
      )}
    </Card>
  );
}
