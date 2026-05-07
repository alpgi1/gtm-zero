"use client";

import { useEffect, useState } from "react";

const PITCH_DURATION_S = 180;

interface Milestone {
  startS: number;
  endS: number;
  label: string;
  consoleMsg: string;
}

const MILESTONES: Milestone[] = [
  { startS: 0,   endS: 35,  label: "HOOK",      consoleMsg: "00:00 — open on Dashboard" },
  { startS: 35,  endS: 115, label: "OBJECTION", consoleMsg: "00:35 — should be on Objection scene" },
  { startS: 115, endS: 150, label: "OUTREACH",  consoleMsg: "01:55 — should be transitioning to Outreach" },
  { startS: 150, endS: 170, label: "DOCUMENTS", consoleMsg: "02:30 — should be on Documents" },
  { startS: 170, endS: 180, label: "CLOSE",     consoleMsg: "02:50 — should be on Dashboard, closing" },
];

function pad(n: number): string {
  return n < 10 ? `0${n}` : String(n);
}

function formatRemaining(remainingS: number): string {
  if (remainingS >= 0) {
    return `${pad(Math.floor(remainingS / 60))}:${pad(remainingS % 60)}`;
  }
  const overS = Math.abs(remainingS);
  return `+${pad(Math.floor(overS / 60))}:${pad(overS % 60)}`;
}

function milestoneFor(elapsedS: number): Milestone {
  if (elapsedS >= PITCH_DURATION_S) return MILESTONES[MILESTONES.length - 1];
  for (const m of MILESTONES) {
    if (elapsedS >= m.startS && elapsedS < m.endS) return m;
  }
  return MILESTONES[0];
}

/**
 * Hidden countdown for solo rehearsal. Activated by ?rehearsal=true on
 * any route. Counts DOWN from 03:00, color-shifts at 60s/30s remaining,
 * milestone label tracks the planned pitch sections, console logs fire on
 * each milestone transition. After 0:00 it counts UP red so you can see
 * exactly how much you ran over.
 */
export function RehearsalOverlay({ enabled }: { enabled: boolean }) {
  const [elapsedS, setElapsedS] = useState(0);
  const [startedAt] = useState(() => Date.now());
  const [lastLoggedMilestone, setLastLoggedMilestone] = useState<string | null>(
    null,
  );

  useEffect(() => {
    if (!enabled) return;
    const id = window.setInterval(() => {
      setElapsedS(Math.floor((Date.now() - startedAt) / 1000));
    }, 250);
    return () => window.clearInterval(id);
  }, [enabled, startedAt]);

  useEffect(() => {
    if (!enabled) return;
    const m = milestoneFor(elapsedS);
    if (m.label !== lastLoggedMilestone && elapsedS >= m.startS) {
      console.info(`[rehearsal] ${m.consoleMsg}`);
      setLastLoggedMilestone(m.label);
    }
    if (elapsedS === PITCH_DURATION_S && lastLoggedMilestone !== "TIMEUP") {
      console.warn("[rehearsal] 03:00 — TIME UP");
      setLastLoggedMilestone("TIMEUP");
    }
  }, [elapsedS, enabled, lastLoggedMilestone]);

  if (!enabled) return null;

  const remainingS = PITCH_DURATION_S - elapsedS;
  const overrun = remainingS < 0;
  const m = milestoneFor(elapsedS);

  let timerColor = "text-text-primary";
  if (overrun) timerColor = "text-critical";
  else if (remainingS < 30) timerColor = "text-critical";
  else if (remainingS < 60) timerColor = "text-warning";

  return (
    <div
      className="fixed bottom-4 right-4 z-50 flex items-center gap-3 rounded-md border border-border-strong bg-bg-elevated px-4 py-2.5 shadow-2xl"
      aria-live="polite"
    >
      <span className="text-[10px] uppercase tracking-wider text-text-tertiary">
        REHEARSAL
      </span>
      <span
        className={`font-mono text-2xl tabular-nums ${timerColor}`}
      >
        {formatRemaining(remainingS)}
      </span>
      <span className="text-[10px] uppercase tracking-wider text-text-tertiary">
        {overrun ? "OVERRUN" : m.label}
      </span>
    </div>
  );
}
