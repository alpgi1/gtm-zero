"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import { ArrowRight, Zap } from "lucide-react";
import { ActivityFeed } from "@/components/primitives/activity-feed";
import { ArchitecturePulse } from "@/components/dashboard/architecture-pulse";
import { LiveTimestamp } from "@/components/primitives/live-timestamp";
import { MetricCard } from "@/components/primitives/metric-card";
import { SuggestionChip } from "@/components/primitives/suggestion-chip";
import { Button } from "@/components/ui/button";
import { fetchDashboardOrSynthetic } from "@/lib/api";
import { useConnectivity } from "@/lib/connectivity";
import { easeOutQuint } from "@/lib/motion";
import { getSyntheticDashboard } from "@/lib/synthetic";
import type { DashboardResponse } from "@/lib/types";

const SUGGESTIONS = [
  "How do you handle GDPR compliance?",
  "What prevents prompt injection?",
  "How long does ERP integration take?",
  "Where is customer data stored?",
  "What's your stance on hallucinations?",
  "Can we deploy on-prem?",
];

const HERO_WORDS: { text: string; italic: boolean }[] = [
  { text: "Sales", italic: false },
  { text: "engineering,", italic: false },
  { text: "answered.", italic: true },
];

function Hero() {
  // First-mount-only guard: any re-render must not retrigger the stagger.
  const animatedOnceRef = useRef(false);
  const [shouldAnimate] = useState(() => {
    if (animatedOnceRef.current) return false;
    animatedOnceRef.current = true;
    return true;
  });

  return (
    <section className="mb-10">
      <h1 className="text-5xl font-medium tracking-tight leading-[1.05] text-text-primary">
        {HERO_WORDS.map((w, i) => (
          <motion.span
            key={i}
            initial={shouldAnimate ? { opacity: 0, y: 6 } : false}
            animate={{ opacity: 1, y: 0 }}
            transition={{
              duration: 0.4,
              delay: i === 2 ? 0.4 : i * 0.08,
              ease: easeOutQuint,
            }}
            className={
              w.italic
                ? "font-serif italic font-normal"
                : "mr-2.5"
            }
            style={{ display: "inline-block" }}
          >
            {w.text}
          </motion.span>
        ))}
      </h1>
      <motion.p
        initial={shouldAnimate ? { opacity: 0 } : false}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.4, delay: 0.8, ease: easeOutQuint }}
        className="mt-4 text-base text-text-secondary max-w-2xl"
      >
        Live AI sales engineer that handles technical objections, drafts
        hyper-personalized outreach, and works your pipeline — while you
        ship product.
      </motion.p>
    </section>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState<DashboardResponse>(() =>
    getSyntheticDashboard(),
  );
  const { setState } = useConnectivity();

  useEffect(() => {
    let cancelled = false;
    fetchDashboardOrSynthetic().then(({ data, online }) => {
      if (cancelled) return;
      setData(data);
      setState(online ? "connected" : "offline");
    });
    return () => {
      cancelled = true;
    };
  }, [setState]);

  return (
    <div>
      <Hero />

      {/* Metrics */}
      <section className="grid grid-cols-3 gap-4 mb-10">
        <MetricCard
          label="Outreach sent"
          value={data.metrics.outreachSent}
          format="integer"
          subtitle="last 30 days"
        />
        <MetricCard
          label="Meetings booked"
          value={data.metrics.meetingsBooked}
          format="integer"
          delta={{ value: data.metrics.meetingsBookedDelta, direction: "up" }}
          subtitle="last 30 days"
        />
        <MetricCard
          label="Pipeline created"
          value={data.metrics.pipelineCreatedEur}
          format="currency-eur"
          subtitle="qualified opportunities"
        />
      </section>

      {/* Action bar */}
      <div className="mb-6 flex items-center justify-between rounded-md border border-border-subtle bg-bg-surface px-5 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-sm bg-bg-elevated">
            <Zap className="h-4 w-4 text-accent" strokeWidth={1.75} />
          </div>
          <div>
            <div className="text-sm font-medium text-text-primary">
              Live objection handling
            </div>
            <div className="text-xs text-text-tertiary">
              Average response time:{" "}
              <span className="font-mono text-text-secondary">
                1.7s first token
              </span>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden font-mono text-[11px] text-text-tertiary md:inline">
            cmd + 1
          </span>
          <Button asChild>
            <Link href="/objection">
              Open Objection Handling
              <ArrowRight className="ml-2 h-3.5 w-3.5" />
            </Link>
          </Button>
        </div>
      </div>

      {/* Activity */}
      <section className="mb-14">
        <div className="mb-3 flex items-center justify-between">
          <span className="text-[11px] font-medium uppercase tracking-wider text-text-tertiary">
            Recent activity
          </span>
          <span className="font-mono text-[11px] text-text-tertiary">
            last sync:{" "}
            <LiveTimestamp format="sync" className="text-text-secondary" />
          </span>
        </div>
        <ActivityFeed items={data.activityFeed} />
      </section>

      {/* Suggestion chips */}
      <section className="mb-12">
        <span className="text-[11px] font-medium uppercase tracking-wider text-text-tertiary">
          What GTM-Zero handles today
        </span>
        <div className="mt-4 flex flex-wrap gap-2">
          {SUGGESTIONS.map((q) => (
            <SuggestionChip
              key={q}
              question={q}
              href={`/objection?prefill=${encodeURIComponent(q)}`}
            />
          ))}
        </div>
      </section>

      {/* Architecture pulse */}
      <ArchitecturePulse />
    </div>
  );
}
