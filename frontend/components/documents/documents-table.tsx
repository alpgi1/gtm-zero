"use client";

import { ChevronRight } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { formatInteger } from "@/lib/format";
import { formatRelativeTime } from "@/lib/format";
import type { DocumentListItem } from "@/lib/types";

interface Props {
  documents: DocumentListItem[];
  onSelect: (id: string) => void;
}

export function DocumentsTable({ documents, onSelect }: Props) {
  if (documents.length === 0) {
    return (
      <Card className="px-5 py-12 text-center">
        <p className="text-sm text-text-secondary">
          No documents ingested yet. Click <span className="font-mono">Sync now</span>{" "}
          to seed the demo corpus.
        </p>
      </Card>
    );
  }

  return (
    <Card className="overflow-hidden">
      {/* Header */}
      <div className="flex h-9 items-center border-b border-border-subtle bg-bg-base px-4">
        <HeaderCell className="flex-1">Title</HeaderCell>
        <HeaderCell className="w-32">Type</HeaderCell>
        <HeaderCell className="w-24 text-right justify-end">Chars</HeaderCell>
        <HeaderCell className="w-20 text-right justify-end">Chunks</HeaderCell>
        <HeaderCell className="w-32">Last embed</HeaderCell>
        <HeaderCell className="w-10" />
      </div>
      {/* Body */}
      <ul className="divide-y divide-border-subtle">
        {documents.map((doc) => (
          <li key={doc.id}>
            <button
              type="button"
              onClick={() => onSelect(doc.id)}
              className="group flex h-12 w-full items-center px-4 transition-colors hover:bg-bg-elevated"
            >
              <span className="flex-1 truncate font-mono text-[13px] text-text-primary text-left">
                {doc.title}
              </span>
              <span className="w-32">
                <Badge variant="default">{doc.sourceType}</Badge>
              </span>
              <span className="w-24 text-right font-mono text-[12px] text-text-secondary">
                {formatInteger(doc.charCount)}
              </span>
              <span className="w-20 text-right font-mono text-[12px] text-text-secondary">
                {doc.chunkCount}
              </span>
              <span className="w-32 truncate text-[12px] text-text-tertiary">
                {doc.ingestedAt ? formatRelativeTime(doc.ingestedAt) : "—"}
              </span>
              <span className="w-10 text-right">
                <ChevronRight className="ml-auto h-3.5 w-3.5 text-text-tertiary opacity-0 transition-opacity group-hover:opacity-100" />
              </span>
            </button>
          </li>
        ))}
      </ul>
    </Card>
  );
}

function HeaderCell({
  className,
  children,
}: {
  className?: string;
  children?: React.ReactNode;
}) {
  return (
    <span
      className={`flex items-center text-[10px] uppercase tracking-wider text-text-tertiary ${className ?? ""}`}
    >
      {children}
    </span>
  );
}
