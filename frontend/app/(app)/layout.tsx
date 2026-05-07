"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { AppShell } from "@/components/shell/app-shell";
import { TopBar } from "@/components/shell/top-bar";
import { RehearsalOverlay } from "@/components/shell/rehearsal-overlay";
import { RadialGlow } from "@/components/primitives/radial-glow";
import { ConnectivityProvider } from "@/lib/connectivity";
import { useWarmup } from "@/lib/use-warmup";

function LayoutInner({ children }: { children: React.ReactNode }) {
  const searchParams = useSearchParams();
  const rehearsal = searchParams?.get("rehearsal") === "true";
  useWarmup();

  return (
    <ConnectivityProvider>
      <RadialGlow />
      <TopBar />
      <AppShell>{children}</AppShell>
      <RehearsalOverlay enabled={rehearsal} />
    </ConnectivityProvider>
  );
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={null}>
      <LayoutInner>{children}</LayoutInner>
    </Suspense>
  );
}
