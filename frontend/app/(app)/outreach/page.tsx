"use client";

import { Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowUpRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { OutreachList } from "@/components/outreach/outreach-list";
import {
  OutreachDetail,
  type DetailState,
} from "@/components/outreach/outreach-detail";
import { GenerateSheet } from "@/components/outreach/generate-sheet";
import {
  fetchOutreach,
  fetchRecentOutreach,
  markOutreachSent,
} from "@/lib/api";
import type {
  OutreachHistoryDto,
  OutreachResponseDto,
} from "@/lib/types";

function OutreachPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const selectedId = searchParams.get("id");

  const [list, setList] = useState<OutreachHistoryDto[] | null>(null);
  const [listError, setListError] = useState<string | null>(null);
  const [detail, setDetail] = useState<OutreachResponseDto | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [sheetOpen, setSheetOpen] = useState(false);

  const loadList = useCallback(async () => {
    try {
      const items = await fetchRecentOutreach(20);
      setList(items);
      setListError(null);
      return items;
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to load outreach";
      setListError(msg);
      setList([]);
      return [];
    }
  }, []);

  // First load: fetch list, then auto-select first item if URL has none.
  useEffect(() => {
    let cancelled = false;
    loadList().then((items) => {
      if (cancelled) return;
      if (items.length > 0 && !selectedId) {
        const first = items[0];
        const params = new URLSearchParams(searchParams.toString());
        params.set("id", first.id);
        router.replace(`/outreach?${params.toString()}`, { scroll: false });
      }
    });
    return () => {
      cancelled = true;
    };
    // intentionally only on mount — selectedId/searchParams updates handled below
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Fetch detail whenever the selected id changes.
  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      setDetailError(null);
      setDetailLoading(false);
      return;
    }
    let cancelled = false;
    setDetailLoading(true);
    setDetailError(null);
    fetchOutreach(selectedId)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
      })
      .catch((e) => {
        if (cancelled) return;
        const msg = e instanceof Error ? e.message : "Failed to load message";
        setDetailError(msg);
      })
      .finally(() => {
        if (!cancelled) setDetailLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedId]);

  const handleSelect = useCallback(
    (id: string) => {
      const params = new URLSearchParams(searchParams.toString());
      params.set("id", id);
      router.push(`/outreach?${params.toString()}`, { scroll: false });
    },
    [router, searchParams],
  );

  const handleMarkSent = useCallback(async () => {
    if (!detail) return;
    try {
      const updated = await markOutreachSent(detail.outreachId);
      setDetail(updated);
      await loadList();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to update status";
      setDetailError(msg);
    }
  }, [detail, loadList]);

  const handleGenerated = useCallback(
    async (resp: OutreachResponseDto) => {
      const items = await loadList();
      const newId = resp.outreachId;
      const params = new URLSearchParams(searchParams.toString());
      params.set("id", newId);
      router.push(`/outreach?${params.toString()}`, { scroll: false });
      // Pre-fill detail to avoid a flash of loading state.
      setDetail(resp);
      setDetailError(null);
      void items;
    },
    [loadList, router, searchParams],
  );

  const handleRetryDetail = useCallback(() => {
    if (!selectedId) return;
    setDetail(null);
    setDetailError(null);
    setDetailLoading(true);
    fetchOutreach(selectedId)
      .then(setDetail)
      .catch((e) => {
        const msg = e instanceof Error ? e.message : "Failed to load message";
        setDetailError(msg);
      })
      .finally(() => setDetailLoading(false));
  }, [selectedId]);

  const detailState: DetailState = useMemo(() => {
    if (!selectedId) return { kind: "empty" };
    if (detailLoading && !detail) return { kind: "loading" };
    if (detailError && !detail)
      return { kind: "error", message: detailError, onRetry: handleRetryDetail };
    if (detail) return { kind: "loaded", message: detail };
    return { kind: "loading" };
  }, [selectedId, detailLoading, detail, detailError, handleRetryDetail]);

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-end justify-between">
        <div>
          <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
            Outreach
          </span>
          <div className="mt-1 flex items-center gap-3">
            <span className="text-2xl font-semibold tracking-tight text-text-primary">
              Pipeline
            </span>
            <PipelineMetricsInline items={list ?? []} />
          </div>
        </div>
        <Button variant="default" onClick={() => setSheetOpen(true)}>
          Generate new
          <ArrowUpRight className="ml-1.5 h-3.5 w-3.5" />
        </Button>
      </div>

      {listError && (
        <div className="mb-4 rounded-sm border border-critical/30 bg-critical/10 px-4 py-2 text-[12px] text-critical">
          {listError}
        </div>
      )}

      <div className="grid grid-cols-[380px_1fr] gap-6">
        <OutreachList
          items={list ?? []}
          selectedId={selectedId}
          onSelect={handleSelect}
        />
        <OutreachDetail state={detailState} onMarkSent={handleMarkSent} />
      </div>

      <GenerateSheet
        open={sheetOpen}
        onOpenChange={setSheetOpen}
        onGenerated={handleGenerated}
      />
    </div>
  );
}

function PipelineMetricsInline({ items }: { items: OutreachHistoryDto[] }) {
  const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
  const thisWeek = items.filter(
    (it) => new Date(it.createdAt).getTime() >= sevenDaysAgo,
  ).length;
  const sent = items.filter((it) => it.status === "SENT_MOCK").length;
  const avgGenMs =
    items.length === 0
      ? 0
      : Math.round(
          items.reduce((acc, it) => acc + (it.generationLatencyMs || 0), 0) /
            items.length,
        );

  return (
    <span className="font-mono text-[11px] text-text-tertiary">
      THIS WEEK <span className="text-text-secondary">{thisWeek}</span>
      <span className="mx-2">·</span>
      SENT <span className="text-text-secondary">{sent}</span>
      <span className="mx-2">·</span>
      AVG GEN{" "}
      <span className="text-text-secondary">
        {avgGenMs > 0 ? `${(avgGenMs / 1000).toFixed(1)}s` : "—"}
      </span>
    </span>
  );
}

export default function OutreachPage() {
  return (
    <Suspense fallback={null}>
      <OutreachPageInner />
    </Suspense>
  );
}
