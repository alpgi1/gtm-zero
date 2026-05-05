import { FileText, LayoutGrid, MessageSquare, Send } from "lucide-react";
import { Logo } from "@/components/shell/logo";
import { NavItem } from "@/components/shell/nav-item";
import { Badge } from "@/components/ui/badge";

export function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 h-screen w-[240px] bg-bg-base border-r border-border-subtle flex flex-col z-10">
      <header className="h-16 flex items-center justify-between px-5 border-b border-border-subtle">
        <Logo />
        <Badge variant="default">v0.1</Badge>
      </header>

      <nav className="flex-1 flex flex-col gap-0.5 px-2 py-3">
        <NavItem href="/" icon={LayoutGrid} label="Dashboard" routeMono="/" />
        <NavItem
          href="/objection"
          icon={MessageSquare}
          label="Objection Handling"
          routeMono="/objection"
        />
        <NavItem href="/outreach" icon={Send} label="Outreach" routeMono="/outreach" />
        <NavItem
          href="/documents"
          icon={FileText}
          label="Documents"
          routeMono="/documents"
        />
      </nav>

      <footer className="px-5 py-4 border-t border-border-subtle flex flex-col gap-1">
        <span className="text-[11px] font-mono text-text-tertiary">
          MVP · Built in Munich
        </span>
        <span className="text-[11px] font-mono text-text-tertiary">
          START Munich · 10.05.2026
        </span>
      </footer>
    </aside>
  );
}
