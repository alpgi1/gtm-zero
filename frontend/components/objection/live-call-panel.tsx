"use client";

import { useEffect, useState } from "react";
import { MessageSquare, MicOff, Settings, VideoOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { OFFLINE_PROSPECT_CONTEXT } from "@/lib/offline-prospect";
import type { ProspectSummaryDto } from "@/lib/types";

interface Props {
  prospect: ProspectSummaryDto;
  questionsAnswered: number;
  context?: string | null;
}

function initials(name: string | null): string {
  if (!name) return "—";
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function formatTimer(seconds: number): string {
  const h = Math.floor(seconds / 3600).toString().padStart(2, "0");
  const m = Math.floor((seconds % 3600) / 60).toString().padStart(2, "0");
  const s = (seconds % 60).toString().padStart(2, "0");
  return `${h}:${m}:${s}`;
}

function stripLinkedinPath(url: string | null): string {
  if (!url) return "—";
  const m = url.match(/linkedin\.com(\/[a-z]+\/[^/?#]+)/i);
  return m ? m[1] : url;
}

export function LiveCallPanel({ prospect, questionsAnswered, context }: Props) {
  const [seconds, setSeconds] = useState(0);

  useEffect(() => {
    const id = window.setInterval(() => setSeconds((s) => s + 1), 1000);
    return () => window.clearInterval(id);
  }, []);

  const ctx = context ?? OFFLINE_PROSPECT_CONTEXT;
  const fullName = prospect.fullName ?? "Unknown";

  return (
    <Card className="flex flex-col overflow-hidden" style={{ minHeight: 520 }}>
      {/* Call header strip */}
      <div className="flex h-10 items-center justify-between border-b border-border-subtle px-3">
        <div className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-critical" />
          <span className="font-mono text-[11px] text-text-secondary">
            LIVE · {formatTimer(seconds)}
          </span>
        </div>
        <div className="flex h-5 w-5 items-center justify-center rounded-sm border border-border-subtle">
          <Settings className="h-3 w-3 text-text-tertiary" strokeWidth={1.5} />
        </div>
      </div>

      {/* Video tile */}
      <div className="p-4">
        <div className="relative flex aspect-[16/10] w-full items-center justify-center rounded-sm bg-bg-elevated">
          <div className="pointer-events-none absolute inset-0 rounded-sm border border-border-subtle" />

          <div className="flex h-20 w-20 items-center justify-center rounded-full border border-border-strong bg-bg-base">
            <span className="font-mono text-2xl text-text-secondary">
              {initials(fullName)}
            </span>
          </div>

          <div className="absolute bottom-2 left-2 flex items-center gap-1.5 rounded-sm bg-bg-base/80 px-2 py-1 backdrop-blur-sm">
            <MicOff className="h-3 w-3 text-text-tertiary" strokeWidth={1.75} />
            <span className="text-[11px] text-text-primary">{fullName}</span>
          </div>

          <div className="absolute right-2 top-2 flex items-center gap-1 rounded-sm bg-bg-base/80 px-2 py-1 backdrop-blur-sm">
            <VideoOff className="h-3 w-3 text-text-tertiary" strokeWidth={1.75} />
            <span className="font-mono text-[10px] text-text-tertiary">camera off</span>
          </div>
        </div>
      </div>

      {/* Identity block */}
      <div className="flex-1 px-4 pb-4">
        <div className="text-[14px] font-medium text-text-primary">{fullName}</div>
        <div className="text-[12px] text-text-secondary">
          {prospect.role ?? "—"}
          {prospect.companyName ? ` · ${prospect.companyName}` : ""}
        </div>

        <div className="my-3 h-px bg-border-subtle" />

        <dl className="grid grid-cols-[80px_1fr] gap-y-1.5 text-[12px]">
          <dt className="text-text-tertiary">LINKEDIN</dt>
          <dd className="font-mono text-text-secondary truncate">
            {stripLinkedinPath(prospect.linkedinUrl)}
          </dd>
          <dt className="text-text-tertiary">COMPANY</dt>
          <dd className="font-mono text-text-secondary truncate">
            {prospect.companyDomain ?? "—"}
          </dd>
          <dt className="text-text-tertiary">INDUSTRY</dt>
          <dd className="text-text-secondary truncate">
            {prospect.techStackSignals.length > 0
              ? prospect.techStackSignals.slice(0, 3).join(" · ")
              : "—"}
          </dd>
          {ctx && (
            <>
              <dt className="text-text-tertiary">CONTEXT</dt>
              <dd className="text-text-secondary">{ctx}</dd>
            </>
          )}
        </dl>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between border-t border-border-subtle p-3">
        <div className="flex items-center gap-1.5">
          <MessageSquare className="h-3.5 w-3.5 text-text-tertiary" strokeWidth={1.75} />
          <span className="font-mono text-[11px] text-text-tertiary">
            questions answered: {questionsAnswered}
          </span>
        </div>
        <Button variant="quiet" size="sm" className="text-[12px]" tabIndex={-1}>
          End call
        </Button>
      </div>
    </Card>
  );
}
