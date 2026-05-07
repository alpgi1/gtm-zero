"use client";

import { useMemo, useState } from "react";
import { MailOpen, Search } from "lucide-react";
import { Card } from "@/components/ui/card";
import { StatusDot } from "@/components/primitives/status-dot";
import { cn } from "@/lib/cn";
import type { OutreachHistoryDto, OutreachStatus } from "@/lib/types";

interface Props {
  items: OutreachHistoryDto[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}

function initials(name: string | null, fallback: string): string {
  const source = name?.trim() || fallback.trim();
  const parts = source.split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function statusVariant(status: OutreachStatus): "neutral" | "success" | "warning" {
  if (status === "SENT_MOCK") return "success";
  if (status === "APPROVED") return "warning";
  return "neutral";
}

export function OutreachList({ items, selectedId, onSelect }: Props) {
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return items;
    return items.filter((it) => {
      const haystack = [
        it.prospectFullName ?? "",
        it.prospectCompany ?? "",
        it.subject ?? "",
      ]
        .join(" ")
        .toLowerCase();
      return haystack.includes(q);
    });
  }, [items, query]);

  return (
    <Card className="flex flex-col overflow-hidden" style={{ minHeight: 620 }}>
      <div className="border-b border-border-subtle p-3">
        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-text-tertiary" />
          <input
            type="text"
            placeholder="Search prospects, companies…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="w-full rounded-sm border border-border-subtle bg-bg-base pl-8 pr-3 py-2 text-sm text-text-primary placeholder:text-text-tertiary focus:border-accent focus:outline-none transition-colors"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {filtered.length === 0 ? (
          <EmptyState hasItems={items.length > 0} />
        ) : (
          <ul className="divide-y divide-border-subtle">
            {filtered.map((item) => {
              const selected = item.id === selectedId;
              return (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => onSelect(item.id)}
                    className={cn(
                      "w-full text-left px-4 py-3 transition-colors",
                      selected
                        ? "bg-bg-elevated border-l-2 border-l-accent"
                        : "border-l-2 border-l-transparent hover:bg-bg-elevated",
                    )}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-bg-elevated border border-border-subtle">
                        <span className="font-mono text-[12px] text-text-secondary">
                          {initials(item.prospectFullName, item.prospectCompany)}
                        </span>
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <span
                            className={cn(
                              "truncate text-sm font-medium",
                              selected ? "text-text-primary" : "text-text-primary",
                            )}
                          >
                            {item.prospectFullName ?? item.prospectCompany}
                          </span>
                          <StatusDot
                            status={statusVariant(item.status)}
                            label={item.status}
                          />
                        </div>
                        <div className="truncate text-xs text-text-tertiary">
                          {item.prospectCompany}
                        </div>
                        <div className="mt-1 truncate text-xs text-text-secondary">
                          {item.subject || "—"}
                        </div>
                      </div>
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </Card>
  );
}

function EmptyState({ hasItems }: { hasItems: boolean }) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3 p-12 text-center">
      <div className="flex h-10 w-10 items-center justify-center rounded-md bg-bg-elevated border border-border-subtle">
        <MailOpen className="h-4 w-4 text-text-tertiary" strokeWidth={1.75} />
      </div>
      <p className="text-sm text-text-secondary max-w-xs">
        {hasItems
          ? "No matches for that search."
          : "No outreach yet. Generate the first message to get started."}
      </p>
    </div>
  );
}
