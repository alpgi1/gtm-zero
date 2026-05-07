"use client";

import { useEffect, useRef } from "react";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

/**
 * Fire-and-forget pre-warm of the embedding + LLM upstreams. Mounted once at
 * the app shell so the user lands on Dashboard while Voyage and Anthropic
 * connections heat up in the background. Q1 GDPR (the demo's first question)
 * benefits the most — TTF drops from ~2.4s to ~1.5-1.7s on the warm path.
 *
 * Errors are swallowed: warmup is an optimization, not a correctness gate.
 */
export function useWarmup() {
  const fired = useRef(false);
  useEffect(() => {
    if (fired.current) return;
    fired.current = true;
    fetch(`${API_BASE}/warmup`, { method: "POST" })
      .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
      .then((data) => console.debug("[warmup]", data))
      .catch((err) =>
        console.debug("[warmup] failed (non-blocking)", err),
      );
  }, []);
}
