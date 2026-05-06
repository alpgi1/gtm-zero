import { AppShell } from "@/components/shell/app-shell";
import { TopBar } from "@/components/shell/top-bar";
import { RadialGlow } from "@/components/primitives/radial-glow";
import { ConnectivityProvider } from "@/lib/connectivity";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <ConnectivityProvider>
      <RadialGlow />
      <TopBar />
      <AppShell>{children}</AppShell>
    </ConnectivityProvider>
  );
}
