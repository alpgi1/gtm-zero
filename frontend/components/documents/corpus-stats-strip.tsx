"use client";

import { Card } from "@/components/ui/card";
import { formatRelativeTime } from "@/lib/format";
import type { DocumentListItem } from "@/lib/types";

interface Props {
  documents: DocumentListItem[];
}

function formatTokens(n: number): string {
  if (n >= 1000) {
    return new Intl.NumberFormat("de-DE", { maximumFractionDigits: 0 }).format(
      Math.round(n),
    );
  }
  return String(Math.round(n));
}

export function CorpusStatsStrip({ documents }: Props) {
  const totalChunks = documents.reduce((s, d) => s + d.chunkCount, 0);
  const totalChars = documents.reduce((s, d) => s + d.charCount, 0);
  const tokenEst = totalChars / 4;

  let mostRecent: string | null = null;
  for (const d of documents) {
    if (!d.ingestedAt) continue;
    if (!mostRecent || new Date(d.ingestedAt) > new Date(mostRecent)) {
      mostRecent = d.ingestedAt;
    }
  }

  return (
    <Card className="mb-6">
      <div className="grid grid-cols-4 divide-x divide-border-subtle">
        <Stat label="DOCUMENTS" value={String(documents.length)} mono />
        <Stat label="CHUNKS" value={String(totalChunks)} mono />
        <Stat label="TOKENS (EST)" value={formatTokens(tokenEst)} mono />
        <Stat
          label="LAST SYNC"
          value={mostRecent ? formatRelativeTime(mostRecent) : "—"}
          subValue="auto"
        />
      </div>
    </Card>
  );
}

function Stat({
  label,
  value,
  subValue,
  mono = false,
}: {
  label: string;
  value: string;
  subValue?: string;
  mono?: boolean;
}) {
  return (
    <div className="px-5 py-4">
      <div className="text-[10px] uppercase tracking-wider text-text-tertiary">
        {label}
      </div>
      <div
        className={`mt-1 text-xl text-text-primary ${
          mono ? "font-mono" : ""
        }`}
      >
        {value}
      </div>
      {subValue && (
        <div className="mt-0.5 text-[11px] text-text-tertiary">{subValue}</div>
      )}
    </div>
  );
}
