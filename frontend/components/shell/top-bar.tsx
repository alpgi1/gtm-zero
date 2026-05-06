"use client";

import { usePathname } from "next/navigation";
import { LiveTimestamp } from "@/components/primitives/live-timestamp";
import { useConnectivity } from "@/lib/connectivity";
import { cn } from "@/lib/cn";

const BREADCRUMB: Record<string, string> = {
  "/": "~/dashboard",
  "/objection": "~/objection · live",
  "/outreach": "~/outreach",
  "/documents": "~/documents",
};

function breadcrumbFor(pathname: string): string {
  if (BREADCRUMB[pathname]) return BREADCRUMB[pathname];
  for (const [prefix, label] of Object.entries(BREADCRUMB)) {
    if (prefix !== "/" && pathname.startsWith(prefix)) return label;
  }
  return "~";
}

export function TopBar() {
  const pathname = usePathname();
  const { state } = useConnectivity();

  const dotClass =
    state === "connected"
      ? "bg-success"
      : state === "offline"
        ? "bg-critical"
        : "bg-text-tertiary";
  const stateLabel =
    state === "connected"
      ? "connected"
      : state === "offline"
        ? "offline"
        : "syncing";

  return (
    <div
      className={cn(
        "fixed top-0 left-[240px] right-0 z-20 h-9",
        "flex items-center justify-between px-6",
        "bg-bg-base/80 backdrop-blur border-b border-border-subtle",
      )}
    >
      <span className="font-mono text-[11px] text-text-tertiary truncate">
        {breadcrumbFor(pathname)}
      </span>

      <div className="flex items-center gap-3 font-mono text-[11px]">
        <LiveTimestamp format="session" className="text-text-secondary tabular-nums" />
        <span className="text-text-tertiary">·</span>
        <span className="inline-flex items-center gap-1.5 text-text-secondary">
          <span className={cn("h-1.5 w-1.5 rounded-pill", dotClass)} />
          {stateLabel}
        </span>
        <span className="text-text-tertiary">·</span>
        <span className="border border-border-subtle rounded-sm px-1.5 py-0.5 text-[10px] text-text-tertiary">
          ⌘K
        </span>
      </div>
    </div>
  );
}
