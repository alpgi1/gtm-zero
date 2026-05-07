"use client";

interface Props {
  questionsCount: number;
  avgTtfMs: number | null;
  avgCoverage: number | null;
}

function Pip({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col items-end leading-tight">
      <span className="text-[10px] uppercase tracking-wider text-text-tertiary">
        {label}
      </span>
      <span className="font-mono text-[13px] text-text-primary">{value}</span>
    </div>
  );
}

export function SessionMetricsBar({
  questionsCount,
  avgTtfMs,
  avgCoverage,
}: Props) {
  const ttf = avgTtfMs == null ? "—" : `${(avgTtfMs / 1000).toFixed(2)}s`;
  const cov =
    avgCoverage == null ? "—" : `${Math.round(avgCoverage * 100)}%`;

  return (
    <div className="flex items-end gap-4">
      <Pip label="QUESTIONS" value={String(questionsCount)} />
      <span className="pb-0.5 font-mono text-[13px] text-text-tertiary">·</span>
      <Pip label="AVG TTF" value={ttf} />
      <span className="pb-0.5 font-mono text-[13px] text-text-tertiary">·</span>
      <Pip label="COVERAGE" value={cov} />
    </div>
  );
}
