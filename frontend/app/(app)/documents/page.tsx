"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, RotateCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CorpusStatsStrip } from "@/components/documents/corpus-stats-strip";
import { DocumentsTable } from "@/components/documents/documents-table";
import { DocumentDetailSheet } from "@/components/documents/document-detail-sheet";
import { AutoSyncNotice } from "@/components/documents/auto-sync-notice";
import { fetchDocuments, reseedDocuments } from "@/lib/api";
import type { DocumentListItem } from "@/lib/types";

const MIN_SYNC_DURATION_MS = 2000;
const MAX_SYNC_WAIT_MS = 8000;

function withTimeout<T>(p: Promise<T>, ms: number): Promise<T | "timeout"> {
  return new Promise((resolve) => {
    let done = false;
    const t = window.setTimeout(() => {
      if (!done) {
        done = true;
        resolve("timeout");
      }
    }, ms);
    p.then(
      (v) => {
        if (!done) {
          done = true;
          window.clearTimeout(t);
          resolve(v);
        }
      },
      () => {
        if (!done) {
          done = true;
          window.clearTimeout(t);
          resolve("timeout");
        }
      },
    );
  });
}

export default function DocumentsPage() {
  const [documents, setDocuments] = useState<DocumentListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);
  const [selectedDocId, setSelectedDocId] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const docs = await fetchDocuments();
      setDocuments(docs);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load documents");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleSyncNow = useCallback(async () => {
    if (isSyncing) return;
    setIsSyncing(true);
    const start = Date.now();
    // Wait for the reseed to finish so the timestamps visibly refresh, but
    // cap at MAX_SYNC_WAIT_MS so the demo never stalls if voyage is slow.
    // Floor at MIN_SYNC_DURATION_MS so a fast reseed still feels like work.
    await withTimeout(reseedDocuments(true), MAX_SYNC_WAIT_MS);
    const elapsed = Date.now() - start;
    if (elapsed < MIN_SYNC_DURATION_MS) {
      await new Promise((r) => setTimeout(r, MIN_SYNC_DURATION_MS - elapsed));
    }
    await load();
    setIsSyncing(false);
  }, [isSyncing, load]);

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-end justify-between">
        <div>
          <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
            Knowledge base
          </span>
          <div className="mt-1 flex items-center gap-3">
            <span className="text-2xl font-semibold tracking-tight text-text-primary">
              Documents
            </span>
          </div>
        </div>
        <Button
          variant="outline"
          onClick={handleSyncNow}
          disabled={isSyncing}
        >
          {isSyncing ? (
            <>
              <Loader2 className="mr-1.5 h-3 w-3 animate-spin" />
              Syncing…
            </>
          ) : (
            <>
              <RotateCw className="mr-1.5 h-3 w-3" />
              Sync now
            </>
          )}
        </Button>
      </div>

      {error && (
        <div className="mb-4 rounded-sm border border-critical/30 bg-critical/10 px-4 py-2 text-[12px] text-critical">
          {error}
        </div>
      )}

      <CorpusStatsStrip documents={documents} />

      {loading ? (
        <div className="rounded-md border border-border-subtle bg-bg-surface px-5 py-8 text-center text-sm text-text-secondary">
          Loading documents…
        </div>
      ) : (
        <DocumentsTable documents={documents} onSelect={setSelectedDocId} />
      )}

      <AutoSyncNotice />

      <DocumentDetailSheet
        documentId={selectedDocId}
        onClose={() => setSelectedDocId(null)}
      />
    </div>
  );
}
