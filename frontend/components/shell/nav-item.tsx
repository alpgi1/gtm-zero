"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/cn";

interface NavItemProps {
  href: string;
  icon: LucideIcon;
  label: string;
  routeMono: string;
}

function isActive(pathname: string, href: string) {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function NavItem({ href, icon: Icon, label, routeMono }: NavItemProps) {
  const pathname = usePathname();
  const active = isActive(pathname, href);

  return (
    <Link
      href={href}
      className={cn(
        "group relative flex h-9 items-center gap-2.5 px-3 rounded-sm border-l-2 transition-colors duration-150",
        active
          ? "border-accent bg-bg-surface text-text-primary"
          : "border-transparent text-text-secondary hover:bg-bg-surface hover:text-text-primary",
      )}
    >
      <Icon size={16} strokeWidth={1.75} />
      <span className="text-sm flex-1">{label}</span>
      <span
        className={cn(
          "text-[11px] font-mono text-text-tertiary opacity-0 transition-opacity duration-150",
          "group-hover:opacity-100",
          active && "opacity-100",
        )}
      >
        {routeMono}
      </span>
    </Link>
  );
}
