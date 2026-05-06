"use client";

import { motion } from "framer-motion";

interface PulseNode {
  label: string;
  timing: string;
}

const NODES: PulseNode[] = [
  { label: "question", timing: "300ms" },
  { label: "embed", timing: "420ms" },
  { label: "retrieve", timing: "38ms" },
  { label: "cite", timing: "inline" },
  { label: "stream", timing: "1.7s ttf" },
];

const PULSE_DURATION = 1.6;
const PULSE_OFFSET = 0.2;

export function ArchitecturePulse() {
  return (
    <section aria-labelledby="arch-pulse-heading">
      <h2
        id="arch-pulse-heading"
        className="text-[11px] font-medium uppercase tracking-wider text-text-tertiary mb-6"
      >
        How GTM-Zero answers
      </h2>

      <div className="relative">
        {/* Connecting line — fades to transparent at both edges */}
        <div
          aria-hidden
          className="absolute left-0 right-0 top-[14px] h-px"
          style={{
            background:
              "linear-gradient(to right, transparent 0%, var(--color-border-strong) 12%, var(--color-border-strong) 88%, transparent 100%)",
          }}
        />

        <ol className="relative grid grid-cols-5 gap-4">
          {NODES.map((node, i) => (
            <li key={node.label} className="flex flex-col items-center gap-3">
              {/* Halo + dot */}
              <motion.div
                className="relative flex h-7 w-7 items-center justify-center rounded-pill border border-accent/30 bg-bg-base"
                animate={{ opacity: [0.4, 1, 0.4] }}
                transition={{
                  duration: PULSE_DURATION,
                  delay: i * PULSE_OFFSET,
                  ease: "easeInOut",
                  repeat: Infinity,
                }}
              >
                <span className="h-2 w-2 rounded-pill bg-accent" />
              </motion.div>
              <div className="flex flex-col items-center gap-0.5">
                <span className="text-[11px] text-text-secondary">{node.label}</span>
                <span className="text-[10.5px] font-mono text-text-tertiary">
                  {node.timing}
                </span>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
