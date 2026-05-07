"use client";

import { useEffect, useRef } from "react";
import { motion } from "framer-motion";
import { Loader2, Zap } from "lucide-react";
import { CitationChip } from "@/components/primitives/citation-chip";
import { Card } from "@/components/ui/card";
import { easeOutQuint } from "@/lib/motion";
import type { CitationDto } from "@/lib/types";

export type AnswerStatus =
  | "idle"
  | "retrieving"
  | "streaming"
  | "complete"
  | "interrupted"
  | "error";

interface Props {
  status: AnswerStatus;
  question: string | null;
  partialAnswer: string;
  citations: CitationDto[];
  ttfMs: number | null;
  totalMs: number | null;
  retrievedCount: number | null;
  citationCoverage: number | null;
  errorMessage: string | null;
  interruptedAtMs?: number | null;
  onRetry?: () => void;
}

function StatusPill({ status }: { status: AnswerStatus }) {
  if (status === "idle") {
    return (
      <span className="flex items-center gap-1.5 rounded-pill border border-border-subtle bg-bg-elevated px-2 py-0.5">
        <span className="h-2 w-2 rounded-full bg-text-tertiary" />
        <span className="font-mono text-[11px] text-text-tertiary">ready</span>
      </span>
    );
  }
  if (status === "retrieving") {
    return (
      <span className="flex items-center gap-1.5 rounded-pill border border-border-subtle bg-bg-elevated px-2 py-0.5">
        <Loader2 className="h-3 w-3 animate-spin text-accent" strokeWidth={2} />
        <span className="font-mono text-[11px] text-text-secondary">retrieving</span>
      </span>
    );
  }
  if (status === "streaming") {
    return (
      <span className="flex items-center gap-1.5 rounded-pill border border-border-subtle bg-bg-elevated px-2 py-0.5">
        <span className="relative flex h-1.5 w-1.5">
          <span className="absolute inline-flex h-full w-full animate-pulse rounded-full bg-accent" />
        </span>
        <span className="font-mono text-[11px] text-text-secondary">streaming</span>
      </span>
    );
  }
  if (status === "complete") {
    return (
      <span className="flex items-center gap-1.5 rounded-pill border border-border-subtle bg-bg-elevated px-2 py-0.5">
        <span className="h-2 w-2 rounded-full bg-success" />
        <span className="font-mono text-[11px] text-text-secondary">complete</span>
      </span>
    );
  }
  if (status === "interrupted") {
    return (
      <span className="flex items-center gap-1.5 rounded-pill border border-border-subtle bg-bg-elevated px-2 py-0.5">
        <span className="h-2 w-2 rounded-full bg-critical" />
        <span className="font-mono text-[11px] text-critical">interrupted</span>
      </span>
    );
  }
  return (
    <span className="flex items-center gap-1.5 rounded-pill border border-border-subtle bg-bg-elevated px-2 py-0.5">
      <span className="h-2 w-2 rounded-full bg-critical" />
      <span className="font-mono text-[11px] text-critical">error</span>
    </span>
  );
}

function AnswerBody({
  text,
  resolveMarkers,
  onMarkerClick,
}: {
  text: string;
  resolveMarkers: boolean;
  onMarkerClick: (marker: string) => void;
}) {
  if (!resolveMarkers) {
    return <p className="whitespace-pre-wrap break-words">{text}</p>;
  }

  const parts: Array<string | { marker: string; key: string }> = [];
  const re = /\[(\d+)\]/g;
  let last = 0;
  let m: RegExpExecArray | null;
  let i = 0;
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) parts.push(text.slice(last, m.index));
    parts.push({ marker: m[0], key: `m-${i++}-${m.index}` });
    last = m.index + m[0].length;
  }
  if (last < text.length) parts.push(text.slice(last));

  return (
    <p className="whitespace-pre-wrap break-words">
      {parts.map((p, idx) => {
        if (typeof p === "string") return <span key={idx}>{p}</span>;
        return (
          <span
            key={p.key}
            data-marker={p.marker}
            className="cursor-pointer font-medium text-accent hover:underline"
            onClick={() => onMarkerClick(p.marker)}
          >
            {p.marker}
          </span>
        );
      })}
    </p>
  );
}

export function AnswerPanel({
  status,
  question,
  partialAnswer,
  citations,
  ttfMs,
  totalMs,
  retrievedCount,
  citationCoverage,
  errorMessage,
  interruptedAtMs = null,
  onRetry,
}: Props) {
  const bodyRef = useRef<HTMLDivElement>(null);
  const chipsRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = bodyRef.current;
    if (!el) return;
    const nearBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 60;
    if (nearBottom) el.scrollTop = el.scrollHeight;
  }, [partialAnswer]);

  const handleMarkerClick = () => {
    chipsRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  };

  const isComplete = status === "complete";
  const showRetrievingBlock = status === "retrieving" && partialAnswer.length === 0;

  return (
    <Card className="flex flex-col overflow-hidden" style={{ minHeight: 520 }}>
      {/* Header strip */}
      <div className="flex h-10 items-center justify-between border-b border-border-subtle px-5">
        <div className="min-w-0 flex-1">
          {question ? (
            <p className="truncate text-sm font-medium text-text-primary">
              {question}
            </p>
          ) : (
            <p className="text-sm text-text-tertiary">Awaiting question…</p>
          )}
        </div>
        <div className="ml-3 shrink-0">
          <StatusPill status={status} />
        </div>
      </div>

      {/* Body */}
      <div ref={bodyRef} className="flex-1 overflow-y-auto">
        {status === "idle" && (
          <div className="flex h-full flex-col items-center justify-center gap-3 p-12 text-center">
            <div className="flex h-10 w-10 items-center justify-center rounded-md border border-border-subtle bg-bg-elevated">
              <Zap className="h-4 w-4 text-text-tertiary" strokeWidth={1.75} />
            </div>
            <p className="max-w-xs text-sm text-text-secondary">
              Pick a question below or type one. The AI engineer will answer with
              citations to your technical documentation.
            </p>
          </div>
        )}

        {showRetrievingBlock && (
          <div className="flex flex-col gap-3 px-5 py-5">
            <div className="flex items-center gap-2">
              <Loader2 className="h-3.5 w-3.5 animate-spin text-accent" strokeWidth={2} />
              <span className="font-mono text-[12px] text-text-secondary">
                Retrieving relevant sources…
              </span>
            </div>
            <div className="h-px w-full bg-border-subtle" />
            <div className="h-3 w-1/2 animate-pulse rounded-sm bg-bg-elevated" />
            <div className="h-3 w-3/4 animate-pulse rounded-sm bg-bg-elevated" />
            <div className="h-3 w-2/3 animate-pulse rounded-sm bg-bg-elevated" />
          </div>
        )}

        {(status === "streaming" ||
          status === "complete" ||
          status === "interrupted") && (
          <div className="px-5 py-5 text-[15px] leading-relaxed text-text-primary">
            <AnswerBody
              text={partialAnswer}
              resolveMarkers={isComplete}
              onMarkerClick={handleMarkerClick}
            />
            {status === "interrupted" && (
              <div className="mt-4 flex items-center justify-between gap-3 rounded-sm border border-critical/30 bg-critical/10 px-3 py-2">
                <div className="flex items-center gap-2">
                  <span className="h-2 w-2 rounded-full bg-critical" />
                  <span className="text-[12px] text-text-primary">
                    Connection interrupted
                    {interruptedAtMs != null
                      ? ` at ${(interruptedAtMs / 1000).toFixed(1)}s`
                      : ""}
                    .
                  </span>
                </div>
                {onRetry && (
                  <button
                    type="button"
                    onClick={onRetry}
                    className="rounded-sm border border-border-strong bg-bg-elevated px-2.5 py-1 text-[11px] font-medium text-text-primary transition-colors hover:bg-bg-surface"
                  >
                    Retry
                  </button>
                )}
              </div>
            )}
          </div>
        )}

        {status === "error" && (
          <div className="px-5 py-5">
            <p className="text-sm text-critical">
              {errorMessage ??
                "Backend unavailable — open the recorded demo as backup."}
            </p>
          </div>
        )}
      </div>

      {/* Citation chips row */}
      {isComplete && citations.length > 0 && (
        <div
          ref={chipsRef}
          className="flex flex-wrap items-center gap-2 border-t border-border-subtle px-5 py-3"
        >
          <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
            Sources
          </span>
          {citations.map((c, i) => (
            <motion.div
              key={c.marker + c.chunkId}
              initial={{ opacity: 0, y: 4 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{
                delay: i * 0.06,
                duration: 0.25,
                ease: easeOutQuint,
              }}
            >
              <CitationChip
                marker={c.marker}
                documentTitle={c.documentTitle}
                chunkIndex={c.chunkIndex}
              />
            </motion.div>
          ))}
        </div>
      )}

      {/* Telemetry footer */}
      <div className="flex h-10 items-center border-t border-border-subtle px-5 py-2.5">
        {status === "error" ? (
          <span className="font-mono text-[12px] text-critical">error</span>
        ) : (
          <span className="font-mono text-[12px] text-text-secondary">
            ttf {fmtMs(ttfMs)} · total {fmtMs(totalMs)} ·{" "}
            {retrievedCount != null ? `${retrievedCount} sources retrieved` : "— sources"} ·{" "}
            {citationCoverage != null
              ? `${Math.round(citationCoverage * 100)}% cited`
              : "— cited"}
          </span>
        )}
      </div>
    </Card>
  );
}

function fmtMs(ms: number | null): string {
  if (ms == null) return "—";
  return `${(ms / 1000).toFixed(2)}s`;
}
