"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ArrowRight } from "lucide-react";
import { ActivityFeed } from "@/components/primitives/activity-feed";
import { MetricCard } from "@/components/primitives/metric-card";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardContent } from "@/components/ui/card";
import { fetchDashboard } from "@/lib/api";
import type { DashboardResponse } from "@/lib/types";

const SUGGESTIONS = [
  "How do you handle GDPR compliance?",
  "What prevents prompt injection?",
  "How long does ERP integration take?",
  "Where is customer data stored?",
  "What's your stance on hallucinations?",
  "Can we deploy on-prem?",
];

const FALLBACK: DashboardResponse = {
  metrics: {
    outreachSent: 127,
    meetingsBooked: 14,
    meetingsBookedDelta: 27,
    pipelineCreatedEur: 1_200_000,
  },
  activityFeed: [],
};

function LoadingBar() {
  return (
    <div className="h-px w-full bg-bg-elevated overflow-hidden relative">
      <div className="absolute inset-y-0 left-0 w-1/3 bg-accent animate-[loading_1.4s_ease-in-out_infinite]" />
      <style jsx>{`
        @keyframes loading {
          0% { transform: translateX(-100%); }
          100% { transform: translateX(400%); }
        }
      `}</style>
    </div>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchDashboard()
      .then((d) => {
        if (!cancelled) setData(d);
      })
      .catch((e) => {
        if (!cancelled) setError(e?.message ?? "Failed to load dashboard");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const view = data ?? FALLBACK;

  return (
    <div>
      {/* Hero */}
      <section className="mb-12">
        <h1 className="text-5xl font-medium tracking-tight leading-[1.05] text-text-primary">
          Sales engineering,{" "}
          <span className="font-serif italic font-normal text-text-primary">
            answered.
          </span>
        </h1>
        <p className="mt-4 text-base text-text-secondary max-w-2xl">
          Live AI sales engineer that handles technical objections, drafts
          hyper-personalized outreach, and works your pipeline — while you
          ship product.
        </p>
      </section>

      {/* Metrics */}
      <section className="grid grid-cols-3 gap-4 mb-12">
        <MetricCard
          label="Outreach sent"
          value={view.metrics.outreachSent}
          format="integer"
          subtitle="last 30 days"
        />
        <MetricCard
          label="Meetings booked"
          value={view.metrics.meetingsBooked}
          format="integer"
          delta={{ value: view.metrics.meetingsBookedDelta, direction: "up" }}
          subtitle="last 30 days"
        />
        <MetricCard
          label="Pipeline created"
          value={view.metrics.pipelineCreatedEur}
          format="currency-eur"
          subtitle="qualified opportunities"
        />
      </section>

      {/* Two-column */}
      <section className="grid grid-cols-3 gap-4">
        <div className="col-span-2 flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <span className="text-[11px] uppercase tracking-wider text-text-tertiary">
              Recent activity
            </span>
            {loading && <span className="w-32"><LoadingBar /></span>}
          </div>
          {error ? (
            <Card className="p-5">
              <p className="text-sm text-text-tertiary">
                Could not reach backend. Showing offline shell.
              </p>
            </Card>
          ) : (
            <ActivityFeed items={view.activityFeed} />
          )}
        </div>

        <div className="col-span-1 flex flex-col gap-3">
          <span className="text-[11px] uppercase tracking-wider text-text-tertiary">
            Try it now
          </span>
          <Card>
            <CardHeader>
              <h2 className="text-lg font-semibold tracking-tight text-text-primary">
                Try it now
              </h2>
              <p className="text-sm text-text-secondary">
                Start a live objection handling session.
              </p>
            </CardHeader>
            <CardContent className="flex flex-col gap-3">
              <Button asChild>
                <Link href="/objection">
                  Open Objection Handling
                  <ArrowRight size={14} />
                </Link>
              </Button>
              <span className="text-[11px] font-mono text-text-tertiary">
                cmd + 1
              </span>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Suggestion chips */}
      <section className="mt-12">
        <span className="text-[11px] uppercase tracking-wider text-text-tertiary">
          What GTM-Zero handles today
        </span>
        <div className="mt-4 flex flex-wrap gap-2">
          {SUGGESTIONS.map((q) => (
            <Link
              key={q}
              href={`/objection?prefill=${encodeURIComponent(q)}`}
              className="inline-flex items-center px-3 py-1.5 rounded-pill bg-bg-elevated border border-border-subtle text-[12px] text-text-secondary hover:text-accent hover:border-accent transition-colors duration-150"
            >
              {q}
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
