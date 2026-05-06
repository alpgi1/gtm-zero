import Link from "next/link";
import { ArrowUpRight } from "lucide-react";

interface SuggestionChipProps {
  question: string;
  href: string;
}

export function SuggestionChip({ question, href }: SuggestionChipProps) {
  return (
    <Link
      href={href}
      className="group inline-flex items-center gap-2 px-3 py-1.5 rounded-pill bg-bg-elevated border border-border-subtle text-[12px] text-text-secondary transition-[transform,colors,border-color] duration-150 hover:-translate-y-px hover:text-accent hover:border-accent/50"
    >
      <span>{question}</span>
      <ArrowUpRight
        size={12}
        strokeWidth={2}
        className="text-text-tertiary transition-colors duration-150 group-hover:text-accent"
      />
    </Link>
  );
}
