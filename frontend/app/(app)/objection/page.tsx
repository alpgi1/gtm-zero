"use client";

import { Suspense, useCallback, useEffect, useReducer, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { LiveCallPanel } from "@/components/objection/live-call-panel";
import { AnswerPanel, type AnswerStatus } from "@/components/objection/answer-panel";
import { QuickFireBar, QUICK_FIRE } from "@/components/objection/quick-fire-bar";
import { SessionMetricsBar } from "@/components/objection/session-metrics-bar";
import { SessionHistory } from "@/components/objection/session-history";
import { OFFLINE_PROSPECT } from "@/lib/offline-prospect";
import {
  fetchProspectByLinkedinUrl,
  streamObjection,
} from "@/lib/objection-stream";
import type {
  CitationDto,
  ObjectionResponseDto,
  ProspectSummaryDto,
} from "@/lib/types";

const DEMO_LINKEDIN_URL =
  process.env.NEXT_PUBLIC_DEMO_PROSPECT_LINKEDIN_URL ?? "";

interface State {
  status: AnswerStatus;
  currentQuestion: string | null;
  partialAnswer: string;
  citations: CitationDto[];
  ttfMs: number | null;
  totalMs: number | null;
  retrievedCount: number | null;
  citationCoverage: number | null;
  errorMessage: string | null;
  interruptedAtMs: number | null;
  history: ObjectionResponseDto[];
}

type Action =
  | { type: "START"; question: string }
  | { type: "RETRIEVED"; citations: CitationDto[] }
  | { type: "TOKEN"; text: string; ttfMs: number | null }
  | { type: "COMPLETED"; response: ObjectionResponseDto; coverage: number }
  | { type: "STREAM_FAILED"; message: string; elapsedMs: number };

const initialState: State = {
  status: "idle",
  currentQuestion: null,
  partialAnswer: "",
  citations: [],
  ttfMs: null,
  totalMs: null,
  retrievedCount: null,
  citationCoverage: null,
  errorMessage: null,
  interruptedAtMs: null,
  history: [],
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "START":
      return {
        ...initialState,
        status: "retrieving",
        currentQuestion: action.question,
        history: state.history,
      };
    case "RETRIEVED":
      return {
        ...state,
        citations: action.citations,
        retrievedCount: action.citations.length,
      };
    case "TOKEN":
      return {
        ...state,
        status: "streaming",
        partialAnswer: state.partialAnswer + action.text,
        ttfMs: state.ttfMs ?? action.ttfMs,
      };
    case "COMPLETED":
      return {
        ...state,
        status: "complete",
        partialAnswer: action.response.answer,
        citations: action.response.citations,
        retrievedCount: action.response.retrievedCount,
        ttfMs: action.response.firstTokenLatencyMs,
        totalMs: action.response.totalLatencyMs,
        citationCoverage: action.coverage,
        history: [...state.history, action.response],
      };
    case "STREAM_FAILED":
      // If tokens already arrived, preserve the partial answer and let the
      // user retry. Otherwise it's a hard error (backend unreachable, etc).
      if (state.partialAnswer.length > 0) {
        return {
          ...state,
          status: "interrupted",
          interruptedAtMs: action.elapsedMs,
        };
      }
      return {
        ...state,
        status: "error",
        errorMessage: action.message,
      };
    default:
      return state;
  }
}

function computeCoverage(answer: string): number {
  const sentences = answer
    .split(/(?<=[.!?])\s+/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0 && /[a-zA-Z]/.test(s));
  if (sentences.length === 0) return 0;
  const cited = sentences.filter((s) => /\[\d+\]/.test(s)).length;
  return cited / sentences.length;
}

function ObjectionPageInner() {
  const searchParams = useSearchParams();
  const prefill = searchParams?.get("prefill") ?? "";

  const [sessionId] = useState(() =>
    typeof crypto !== "undefined" && crypto.randomUUID
      ? crypto.randomUUID()
      : `session-${Date.now()}`,
  );
  const [prospect, setProspect] = useState<ProspectSummaryDto>(OFFLINE_PROSPECT);
  const [customQuestion, setCustomQuestion] = useState(prefill);
  const [pressedNumber, setPressedNumber] = useState<number | null>(null);
  const [state, dispatch] = useReducer(reducer, initialState);
  const abortRef = useRef<AbortController | null>(null);
  const requestStartRef = useRef<number>(0);

  // Fetch prospect on mount
  useEffect(() => {
    if (!DEMO_LINKEDIN_URL) return;
    let cancelled = false;
    fetchProspectByLinkedinUrl(DEMO_LINKEDIN_URL)
      .then((p) => {
        if (cancelled) return;
        if (p) {
          setProspect(p);
        } else if (process.env.NODE_ENV === "development") {
          // 404 from the prospects lookup — frontend silently falls back
          // to OFFLINE_PROSPECT (Marie Dubois). In dev mode we surface this
          // because a typo in NEXT_PUBLIC_DEMO_PROSPECT_LINKEDIN_URL is the
          // most common cause and is silent in prod.
          console.warn(
            `[GTM-Zero] Demo prospect not found for URL: ${DEMO_LINKEDIN_URL}\n` +
              `Using OFFLINE_PROSPECT fallback (Marie Dubois).\n` +
              `If you intended to load a real prospect, verify URL match in the prospects table.`,
          );
        }
      })
      .catch(() => {
        // Network/CORS failure — keep OFFLINE_PROSPECT silently.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const isStreaming = state.status === "retrieving" || state.status === "streaming";

  const ask = useCallback(
    (question: string) => {
      if (isStreaming) return;
      // Cancel any inflight (defensive)
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;
      requestStartRef.current = performance.now();

      dispatch({ type: "START", question });

      streamObjection(
        { question, sessionId },
        {
          onRetrieved: (e) => {
            dispatch({ type: "RETRIEVED", citations: e.citations });
          },
          onToken: (text) => {
            const ttf =
              requestStartRef.current > 0
                ? Math.round(performance.now() - requestStartRef.current)
                : null;
            dispatch({ type: "TOKEN", text, ttfMs: ttf });
          },
          onCompleted: (response) => {
            const cov = computeCoverage(response.answer);
            dispatch({ type: "COMPLETED", response, coverage: cov });
          },
          onFailed: (e) => {
            const elapsedMs =
              requestStartRef.current > 0
                ? Math.round(performance.now() - requestStartRef.current)
                : 0;
            dispatch({
              type: "STREAM_FAILED",
              message: e.message,
              elapsedMs,
            });
          },
        },
        controller.signal,
      ).catch((err) => {
        if (controller.signal.aborted) return;
        const elapsedMs =
          requestStartRef.current > 0
            ? Math.round(performance.now() - requestStartRef.current)
            : 0;
        dispatch({
          type: "STREAM_FAILED",
          message:
            err instanceof Error
              ? `Backend unavailable — ${err.message}`
              : "Backend unavailable — open the recorded demo as backup.",
          elapsedMs,
        });
      });
    },
    [isStreaming, sessionId],
  );

  // Keyboard shortcuts: 1, 2, 3
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === "INPUT" ||
          target.tagName === "TEXTAREA" ||
          target.isContentEditable)
      ) {
        return;
      }
      if (e.metaKey || e.ctrlKey || e.altKey) return;
      if (e.key !== "1" && e.key !== "2" && e.key !== "3") return;
      const num = parseInt(e.key, 10) as 1 | 2 | 3;
      const q = QUICK_FIRE.find((qq) => qq.number === num);
      if (!q) return;
      if (isStreaming) return;
      e.preventDefault();
      setPressedNumber(num);
      window.setTimeout(() => setPressedNumber(null), 100);
      ask(q.question);
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [ask, isStreaming]);

  // Cancel inflight on unmount
  useEffect(
    () => () => {
      abortRef.current?.abort();
    },
    [],
  );

  // Session metrics
  const questionsCount = state.history.length;
  const avgTtfMs =
    questionsCount === 0
      ? null
      : Math.round(
          state.history.reduce((s, h) => s + h.firstTokenLatencyMs, 0) /
            questionsCount,
        );
  const avgCoverage =
    questionsCount === 0
      ? null
      : state.history.reduce((s, h) => s + computeCoverage(h.answer), 0) /
        questionsCount;

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-end justify-between">
        <div>
          <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
            Objection Handling
          </span>
          <div className="mt-1 flex items-center gap-2">
            <span className="text-2xl font-semibold tracking-tight text-text-primary">
              Live session
            </span>
            <span className="flex items-center gap-1.5 rounded-pill border border-border-subtle bg-bg-elevated px-2 py-0.5">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent opacity-60" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-accent" />
              </span>
              <span className="font-mono text-[11px] text-text-secondary">
                recording
              </span>
            </span>
          </div>
        </div>
        <SessionMetricsBar
          questionsCount={questionsCount}
          avgTtfMs={avgTtfMs}
          avgCoverage={avgCoverage}
        />
      </div>

      {/* Split */}
      <div className="grid grid-cols-[360px_1fr] gap-6">
        <LiveCallPanel
          prospect={prospect}
          questionsAnswered={questionsCount}
          context={null}
        />
        <AnswerPanel
          status={state.status}
          question={state.currentQuestion}
          partialAnswer={state.partialAnswer}
          citations={state.citations}
          ttfMs={state.ttfMs}
          totalMs={state.totalMs}
          retrievedCount={state.retrievedCount}
          citationCoverage={state.citationCoverage}
          errorMessage={state.errorMessage}
          interruptedAtMs={state.interruptedAtMs}
          onRetry={() => {
            if (state.currentQuestion) ask(state.currentQuestion);
          }}
        />
      </div>

      {/* Quick-fire */}
      <div className="mt-6">
        <QuickFireBar
          isStreaming={isStreaming}
          customQuestion={customQuestion}
          onCustomChange={setCustomQuestion}
          onAsk={(q) => {
            ask(q);
            setCustomQuestion("");
          }}
          pressedNumber={pressedNumber}
        />
      </div>

      <SessionHistory history={state.history} />
    </div>
  );
}

export default function ObjectionPage() {
  return (
    <Suspense fallback={null}>
      <ObjectionPageInner />
    </Suspense>
  );
}
