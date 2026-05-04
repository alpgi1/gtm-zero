"use client";

import { useEffect, useState } from "react";
import { checkHealth } from "@/lib/api";

type ApiStatus = "checking" | "connected" | "disconnected";

export default function Home() {
  const [apiStatus, setApiStatus] = useState<ApiStatus>("checking");

  useEffect(() => {
    checkHealth()
      .then((h) => setApiStatus(h.status === "UP" ? "connected" : "disconnected"))
      .catch(() => setApiStatus("disconnected"));
  }, []);

  return (
    <main className="min-h-screen flex flex-col items-center justify-center gap-6 bg-zinc-950 text-zinc-100 px-4">
      <div className="flex flex-col items-center gap-3">
        <h1 className="text-6xl font-black tracking-tight text-white">
          GTM-Zero
        </h1>
        <p className="text-xl text-zinc-400 font-light">
          AI Sales Engineer for Technical Founders
        </p>
      </div>

      <div className="mt-4">
        {apiStatus === "checking" && (
          <span className="text-sm text-zinc-500">API: checking…</span>
        )}
        {apiStatus === "connected" && (
          <span className="text-sm text-emerald-400 font-medium">
            API: Connected ✓
          </span>
        )}
        {apiStatus === "disconnected" && (
          <span className="text-sm text-red-400 font-medium">
            API: Disconnected ✗
          </span>
        )}
      </div>

      <div className="mt-8 text-xs text-zinc-700 tracking-widest uppercase">
        Under construction · Part 1 / 10
      </div>
    </main>
  );
}
