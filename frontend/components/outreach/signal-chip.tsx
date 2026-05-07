"use client";

interface SignalChipProps {
  label: string;
  mono?: boolean;
}

/**
 * Read-only kin to CitationChip — used in the Outreach detail panel to
 * render prospect signals (domain, LinkedIn, tech stack tags). No marker,
 * no chunk pointer; just a single subtle pill.
 */
export function SignalChip({ label, mono = false }: SignalChipProps) {
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded-pill bg-bg-elevated border border-border-subtle text-[11px] ${
        mono ? "font-mono text-text-secondary" : "text-text-secondary"
      }`}
    >
      {label}
    </span>
  );
}
