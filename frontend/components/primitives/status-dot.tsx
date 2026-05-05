import { cn } from "@/lib/cn";

type Status = "success" | "critical" | "warning" | "neutral";

const STATUS_COLOR: Record<Status, string> = {
  success: "bg-success",
  critical: "bg-critical",
  warning: "bg-warning",
  neutral: "bg-text-tertiary",
};

export function StatusDot({ status, label }: { status: Status; label: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className={cn("h-1.5 w-1.5 rounded-pill", STATUS_COLOR[status])} />
      <span className="text-[12px] font-mono text-text-secondary">{label}</span>
    </span>
  );
}
