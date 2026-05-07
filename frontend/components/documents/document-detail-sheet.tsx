"use client";

import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { fetchDocumentDetail } from "@/lib/api";
import { formatInteger, formatRelativeTime } from "@/lib/format";
import type { DocumentDetail } from "@/lib/types";

interface Props {
  documentId: string | null;
  onClose: () => void;
}

function formatIngestedTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const yyyy = d.getUTCFullYear();
  const mm = String(d.getUTCMonth() + 1).padStart(2, "0");
  const dd = String(d.getUTCDate()).padStart(2, "0");
  const hh = String(d.getUTCHours()).padStart(2, "0");
  const mi = String(d.getUTCMinutes()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd} ${hh}:${mi} UTC`;
}

export function DocumentDetailSheet({ documentId, onClose }: Props) {
  const open = documentId !== null;
  const [detail, setDetail] = useState<DocumentDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!documentId) {
      setDetail(null);
      setError(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchDocumentDetail(documentId)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((e) => {
        if (cancelled) return;
        setError(e instanceof Error ? e.message : "Failed to load document");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [documentId]);

  return (
    <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
      <SheetContent className="w-[560px]">
        <SheetHeader>
          <SheetTitle className="font-mono text-[15px]">
            {detail?.title ?? "Loading…"}
          </SheetTitle>
          {detail && (
            <p className="mt-1 font-mono text-[11px] text-text-tertiary">
              {detail.sourceType} · {detail.chunkCount} chunks ·{" "}
              {formatInteger(detail.charCount)} chars · ingested{" "}
              {detail.ingestedAt ? formatRelativeTime(detail.ingestedAt) : "—"}
            </p>
          )}
        </SheetHeader>

        {loading && !detail && (
          <div className="flex flex-1 items-center justify-center p-12">
            <Loader2 className="h-4 w-4 animate-spin text-accent" />
          </div>
        )}

        {error && !detail && (
          <div className="flex flex-1 items-center justify-center p-12">
            <p className="text-sm text-critical">{error}</p>
          </div>
        )}

        {detail && (
          <>
            {/* Source info */}
            <div className="border-b border-border-subtle p-5">
              <dl className="grid grid-cols-[120px_1fr] gap-y-2 text-[12px]">
                <dt className="font-mono uppercase tracking-wider text-text-tertiary">
                  source path
                </dt>
                <dd className="font-mono text-text-secondary truncate">
                  {detail.sourcePath ?? "—"}
                </dd>
                <dt className="font-mono uppercase tracking-wider text-text-tertiary">
                  source type
                </dt>
                <dd className="font-mono text-text-secondary">
                  {detail.sourceType}
                </dd>
                <dt className="font-mono uppercase tracking-wider text-text-tertiary">
                  ingested
                </dt>
                <dd className="font-mono text-text-secondary">
                  {detail.ingestedAt
                    ? formatIngestedTime(detail.ingestedAt)
                    : "—"}
                </dd>
              </dl>
            </div>

            {/* Chunk previews */}
            <div className="flex-1 overflow-y-auto px-5 py-5">
              <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
                Chunk previews
              </span>
              <div className="mt-3 space-y-3">
                {detail.chunks.map((chunk) => (
                  <div
                    key={chunk.id}
                    className="border-l-2 border-border-strong bg-bg-base px-4 py-3"
                  >
                    <div className="mb-2 flex items-center justify-between">
                      <span className="font-mono text-[11px] text-text-tertiary">
                        chunk_{chunk.chunkIndex}
                        {chunk.tokenCount != null
                          ? ` · ~${chunk.tokenCount} tokens`
                          : ""}
                      </span>
                      <span className="font-mono text-[11px] text-text-tertiary">
                        [{chunk.id.slice(0, 8)}]
                      </span>
                    </div>
                    <p className="text-[12px] leading-relaxed text-text-secondary line-clamp-4">
                      {chunk.snippet}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}
