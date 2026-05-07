"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronDown } from "lucide-react";
import { CitationChip } from "@/components/primitives/citation-chip";
import { easeOutQuint } from "@/lib/motion";
import type { ObjectionResponseDto } from "@/lib/types";

interface Props {
  history: ObjectionResponseDto[];
}

function fmtMs(ms: number | null | undefined): string {
  if (ms == null) return "—";
  return `${(ms / 1000).toFixed(2)}s`;
}

function computeCoverage(answer: string): number {
  // Match validator's spirit: sentences with at least one [n] marker / total sentences.
  const sentences = answer
    .split(/(?<=[.!?])\s+/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0 && /[a-zA-Z]/.test(s));
  if (sentences.length === 0) return 0;
  const cited = sentences.filter((s) => /\[\d+\]/.test(s)).length;
  return cited / sentences.length;
}

function HistoryRow({
  index,
  item,
}: {
  index: number;
  item: ObjectionResponseDto;
}) {
  const [open, setOpen] = useState(false);
  const coverage = Math.round(computeCoverage(item.answer) * 100);

  return (
    <div className="border-b border-border-subtle last:border-b-0">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left transition-colors hover:bg-bg-elevated"
      >
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="font-mono text-[11px] text-text-tertiary">
              [Q{index}]
            </span>
            <span className="truncate text-sm text-text-primary">
              {item.question}
            </span>
          </div>
          <div className="mt-1 font-mono text-[11px] text-text-tertiary">
            ttf {fmtMs(item.firstTokenLatencyMs)} · total{" "}
            {fmtMs(item.totalLatencyMs)} · {item.retrievedCount} sources ·{" "}
            {coverage}% cited
          </div>
        </div>
        <ChevronDown
          className={`h-4 w-4 shrink-0 text-text-tertiary transition-transform duration-200 ${
            open ? "rotate-180" : ""
          }`}
          strokeWidth={1.75}
        />
      </button>

      <AnimatePresence initial={false}>
        {open && (
          <motion.div
            key="content"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.25, ease: easeOutQuint }}
            className="overflow-hidden"
          >
            <div className="px-4 pb-4">
              <p className="whitespace-pre-wrap text-[14px] leading-relaxed text-text-primary">
                {item.answer}
              </p>
              {item.citations.length > 0 && (
                <div className="mt-3 flex flex-wrap items-center gap-2 border-t border-border-subtle pt-3">
                  <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
                    Sources
                  </span>
                  {item.citations.map((c) => (
                    <CitationChip
                      key={c.marker + c.chunkId}
                      marker={c.marker}
                      documentTitle={c.documentTitle}
                      chunkIndex={c.chunkIndex}
                    />
                  ))}
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export function SessionHistory({ history }: Props) {
  if (history.length === 0) return null;

  // Most-recent first
  const ordered = [...history].reverse();

  return (
    <section className="mt-10">
      <div className="mb-3 flex items-center justify-between">
        <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
          Session history
        </span>
        <span className="font-mono text-[11px] text-text-tertiary">
          {history.length} answered
        </span>
      </div>
      <div className="rounded-md border border-border-subtle bg-bg-surface">
        {ordered.map((item, idx) => (
          <HistoryRow
            key={item.queryId}
            index={history.length - idx}
            item={item}
          />
        ))}
      </div>
    </section>
  );
}
