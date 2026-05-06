"use client";

import { useEffect, useRef, useState } from "react";
import { animate, useMotionValue, useMotionValueEvent } from "framer-motion";
import { ArrowUpRight, ArrowDownRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import { LiveTimestamp } from "@/components/primitives/live-timestamp";
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

const JITTER_DELTAS = [0, 0, 0, 1, -1, 1, 2, 0];
const JITTER_MAX_DRIFT = 5;
const JITTER_INTERVAL_MS = 30_000;

function formatValue(value: number, format: MetricFormat): string {
  return format === "currency-eur"
    ? formatCurrencyEur(value)
    : formatInteger(value);
}

export function MetricCard({
  label,
  value,
  format,
  delta,
  subtitle,
}: MetricCardProps) {
  const motionValue = useMotionValue(0);
  const [display, setDisplay] = useState<string>(formatValue(0, format));
  const currentRef = useRef<number>(0);

  useMotionValueEvent(motionValue, "change", (latest) => {
    currentRef.current = latest;
    setDisplay(formatValue(latest, format));
  });

  // Initial 0 → value count-up
  useEffect(() => {
    const controls = animate(motionValue, value, {
      duration: slow,
      ease: easeOutQuint,
    });
    return () => controls.stop();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  // ±1 jitter every 30s after the initial reveal completes.
  // Capped at ±max-drift so a long-open tab doesn't drift unbounded.
  useEffect(() => {
    if (format === "currency-eur") return; // jitter only on integer counts
    const id = setInterval(() => {
      const d = JITTER_DELTAS[Math.floor(Math.random() * JITTER_DELTAS.length)];
      if (d === 0) return;
      const current = currentRef.current;
      const next = Math.min(value + JITTER_MAX_DRIFT, Math.max(value, current + d));
      animate(motionValue, next, { duration: 0.6, ease: easeOutQuint });
    }, JITTER_INTERVAL_MS);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, format]);

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
      <span className="text-[10.5px] font-mono text-text-tertiary">
        synced <LiveTimestamp format="sync" />
      </span>
    </Card>
  );
}
