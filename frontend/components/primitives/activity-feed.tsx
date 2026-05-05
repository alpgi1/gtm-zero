"use client";

import {
  CheckCircle2,
  ChevronRight,
  FileText,
  MessageSquare,
  Send,
  type LucideIcon,
} from "lucide-react";
import { Card } from "@/components/ui/card";
import { CitationChip } from "@/components/primitives/citation-chip";
import { formatRelativeTime } from "@/lib/format";
import type { DashboardActivityItem, DashboardEventType } from "@/lib/types";

const EVENT_ICON: Record<DashboardEventType, LucideIcon> = {
  OBJECTION_ANSWERED: MessageSquare,
  OUTREACH_GENERATED: Send,
  OUTREACH_SENT_MOCK: CheckCircle2,
  DOCUMENT_INGESTED: FileText,
};

function ActivityFeedItem({ item }: { item: DashboardActivityItem }) {
  const Icon = EVENT_ICON[item.eventType] ?? MessageSquare;
  const showCitations =
    item.eventType === "OBJECTION_ANSWERED" && item.citations.length > 0;

  return (
    <div className="px-5 py-3.5 flex items-start gap-3 hover:bg-bg-surface transition-colors duration-150">
      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-sm bg-bg-elevated text-text-secondary">
        <Icon size={14} strokeWidth={1.75} />
      </span>
      <div className="flex-1 min-w-0">
        <p className="text-sm text-text-primary truncate">{item.summary}</p>
        <p className="text-xs text-text-tertiary mt-0.5">
          {formatRelativeTime(item.createdAt)}
        </p>
        {showCitations && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {item.citations.slice(0, 2).map((c) => (
              <CitationChip
                key={`${item.id}-${c.marker}`}
                marker={c.marker}
                documentTitle={c.documentTitle}
                chunkIndex={c.chunkIndex}
              />
            ))}
          </div>
        )}
      </div>
      <ChevronRight size={14} className="mt-1 text-text-tertiary shrink-0" />
    </div>
  );
}

export function ActivityFeed({ items }: { items: DashboardActivityItem[] }) {
  if (items.length === 0) {
    return (
      <Card className="p-0">
        <div className="px-5 py-8 text-sm text-text-tertiary">
          No recent activity yet
        </div>
      </Card>
    );
  }
  return (
    <Card className="p-0 divide-y divide-border-subtle">
      {items.map((item) => (
        <ActivityFeedItem key={item.id} item={item} />
      ))}
    </Card>
  );
}
