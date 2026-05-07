"use client";

import { useState } from "react";
import { Check, Copy, Lightbulb, MailOpen, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { StatusDot } from "@/components/primitives/status-dot";
import { SignalChip } from "@/components/outreach/signal-chip";
import { formatRelativeTime } from "@/lib/format";
import type { OutreachResponseDto, OutreachStatus } from "@/lib/types";

interface Props {
  state: DetailState;
  onMarkSent: () => void;
}

export type DetailState =
  | { kind: "empty" }
  | { kind: "loading" }
  | { kind: "loaded"; message: OutreachResponseDto }
  | { kind: "error"; message: string; onRetry: () => void };

function initials(name: string | null, fallback: string): string {
  const source = (name && name.trim()) || fallback;
  const parts = source.split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function statusVariant(status: OutreachStatus): "neutral" | "success" | "warning" {
  if (status === "SENT_MOCK") return "success";
  if (status === "APPROVED") return "warning";
  return "neutral";
}

function stripLinkedinPath(url: string | null): string | null {
  if (!url) return null;
  const m = url.match(/linkedin\.com(\/[a-z]+\/[^/?#]+)/i);
  return m ? m[1] : url;
}

export function OutreachDetail({ state, onMarkSent }: Props) {
  return (
    <Card
      className="flex flex-col overflow-hidden"
      style={{ minHeight: 620 }}
    >
      {state.kind === "empty" && <EmptyView />}
      {state.kind === "loading" && <LoadingView />}
      {state.kind === "error" && (
        <ErrorView message={state.message} onRetry={state.onRetry} />
      )}
      {state.kind === "loaded" && (
        <LoadedView message={state.message} onMarkSent={onMarkSent} />
      )}
    </Card>
  );
}

function EmptyView() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 p-12 text-center">
      <div className="flex h-10 w-10 items-center justify-center rounded-md bg-bg-elevated border border-border-subtle">
        <MailOpen className="h-4 w-4 text-text-tertiary" strokeWidth={1.75} />
      </div>
      <p className="text-sm text-text-secondary max-w-xs">
        Select a message to view the full draft, prospect signals, and
        generation metadata.
      </p>
    </div>
  );
}

function LoadingView() {
  return (
    <div className="flex flex-col gap-3 px-5 py-5">
      <div className="h-3 w-1/2 animate-pulse rounded-sm bg-bg-elevated" />
      <div className="h-3 w-3/4 animate-pulse rounded-sm bg-bg-elevated" />
      <div className="h-3 w-2/3 animate-pulse rounded-sm bg-bg-elevated" />
    </div>
  );
}

function ErrorView({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 p-12 text-center">
      <p className="text-sm text-critical">{message}</p>
      <Button variant="outline" size="sm" onClick={onRetry}>
        Retry
      </Button>
    </div>
  );
}

function LoadedView({
  message,
  onMarkSent,
}: {
  message: OutreachResponseDto;
  onMarkSent: () => void;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      const text = `Subject: ${message.subject}\n\n${message.body}`;
      await navigator.clipboard.writeText(text);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1200);
    } catch {
      // Best-effort; older browsers / non-secure contexts may reject.
    }
  };

  const linkedinShort = stripLinkedinPath(message.prospectLinkedinUrl);
  const fullName = message.prospectFullName ?? message.prospectCompany;

  return (
    <>
      {/* Section 1 — Prospect summary */}
      <div className="border-b border-border-subtle p-5">
        <div className="flex items-start gap-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-bg-elevated border border-border-subtle">
            <span className="font-mono text-[13px] text-text-secondary">
              {initials(message.prospectFullName, message.prospectCompany)}
            </span>
          </div>
          <div className="min-w-0 flex-1">
            <div className="text-sm font-medium text-text-primary">{fullName}</div>
            <div className="text-xs text-text-secondary mt-0.5">
              {message.prospectRole ?? "—"}
              {message.prospectCompany ? ` · ${message.prospectCompany}` : ""}
            </div>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {message.prospectCompanyDomain && (
                <SignalChip label={message.prospectCompanyDomain} mono />
              )}
              {linkedinShort && <SignalChip label={linkedinShort} mono />}
              {message.usedSignals.slice(0, 6).map((s) => (
                <SignalChip key={s} label={s} />
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Section 2 — Subject */}
      <div className="border-b border-border-subtle px-5 py-4">
        <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
          Subject
        </span>
        <h2 className="mt-1.5 text-base font-medium text-text-primary">
          {message.subject}
        </h2>
      </div>

      {/* Section 3 — Personalization basis */}
      <div className="border-b border-border-subtle bg-accent-muted/55 px-5 py-3">
        <div className="flex items-start gap-2">
          <Lightbulb
            className="mt-0.5 h-3.5 w-3.5 shrink-0 text-accent"
            strokeWidth={1.75}
          />
          <div className="flex flex-col gap-0.5">
            <span className="text-[11px] font-medium uppercase tracking-wider text-accent">
              Personalization basis
            </span>
            <span className="text-[13px] text-text-primary">
              {message.personalizationBasis && message.personalizationBasis.trim().length > 0
                ? message.personalizationBasis
                : "Not recorded"}
            </span>
          </div>
        </div>
      </div>

      {/* Section 4 — Body */}
      <div className="flex-1 overflow-y-auto px-5 py-5">
        <div className="max-w-prose whitespace-pre-wrap text-[14px] leading-relaxed text-text-primary">
          {message.body}
        </div>
      </div>

      {/* Section 5 — Telemetry */}
      <div className="flex h-9 items-center border-t border-border-subtle px-5 py-2.5">
        <span className="font-mono text-[11px] text-text-tertiary">
          gen {(message.generationLatencyMs / 1000).toFixed(1)}s · model{" "}
          {message.model || "—"} · prompt {message.generationPromptVersion || "—"} ·
          created {formatRelativeTime(message.createdAt)}
        </span>
      </div>

      {/* Section 6 — Action footer */}
      <div className="flex items-center justify-between border-t border-border-subtle px-5 py-3">
        <StatusDot
          status={statusVariant(message.status)}
          label={message.status}
        />
        <div className="flex items-center gap-2">
          <Button variant="quiet" size="sm" onClick={handleCopy}>
            {copied ? (
              <>
                <Check className="mr-1.5 h-3 w-3" />
                copied
              </>
            ) : (
              <>
                <Copy className="mr-1.5 h-3 w-3" />
                Copy
              </>
            )}
          </Button>
          {message.status === "GENERATED" && (
            <Button variant="default" size="sm" onClick={onMarkSent}>
              Mark as sent
              <Send className="ml-1.5 h-3 w-3" />
            </Button>
          )}
          {message.status === "SENT_MOCK" && (
            <span className="font-mono text-[11px] text-success">● sent</span>
          )}
        </div>
      </div>
    </>
  );
}
