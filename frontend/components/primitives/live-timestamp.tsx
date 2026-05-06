"use client";

import { useEffect, useState } from "react";

type Format = "sync" | "session";

interface LiveTimestampProps {
  /** "sync" → "just now / 5s ago / 2m ago"; "session" → "00:14:22". */
  format?: Format;
  /** When provided, anchors elapsed time. Defaults to component mount time. */
  anchor?: number;
  className?: string;
}

function pad(n: number) {
  return n < 10 ? `0${n}` : String(n);
}

/**
 * Reusable client-side ticking timestamp.
 * Updates once per second. No SSR drift — render is gated on first effect tick.
 */
export function LiveTimestamp({
  format = "sync",
  anchor,
  className,
}: LiveTimestampProps) {
  const [mountedAt] = useState(() => anchor ?? Date.now());
  const [, setTick] = useState(0);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    setHydrated(true);
    const id = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(id);
  }, []);

  if (!hydrated) {
    return (
      <span className={className}>
        {format === "session" ? "00:00:00" : "just now"}
      </span>
    );
  }

  const elapsedSec = Math.max(0, Math.floor((Date.now() - mountedAt) / 1000));

  if (format === "session") {
    const h = Math.floor(elapsedSec / 3600);
    const m = Math.floor((elapsedSec % 3600) / 60);
    const s = elapsedSec % 60;
    return <span className={className}>{`${pad(h)}:${pad(m)}:${pad(s)}`}</span>;
  }

  if (elapsedSec < 5) return <span className={className}>just now</span>;
  if (elapsedSec < 60) return <span className={className}>{elapsedSec}s ago</span>;
  const m = Math.floor(elapsedSec / 60);
  return <span className={className}>{m}m ago</span>;
}
