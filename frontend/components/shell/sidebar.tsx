import { FileText, LayoutGrid, MessageSquare, Send } from "lucide-react";
import { Logo } from "@/components/shell/logo";
import { NavItem } from "@/components/shell/nav-item";
import { Badge } from "@/components/ui/badge";

export function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 h-screen w-[240px] bg-bg-base border-r border-border-subtle flex flex-col z-30">
      <header className="h-16 flex items-center justify-between pl-5 pr-4 border-b border-border-subtle">
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

      <footer className="px-3 py-3 border-t border-border-subtle flex flex-col gap-0.5">
        <span className="text-[10.5px] font-mono text-text-tertiary leading-relaxed">
          MVP · Munich
        </span>
        <span className="text-[10.5px] font-mono text-text-tertiary leading-relaxed">
          START · 10.05.26
        </span>
      </footer>
    </aside>
  );
}
