"use client";

import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export interface QuickFireQuestion {
  number: 1 | 2 | 3;
  label: string;
  question: string;
}

export const QUICK_FIRE: QuickFireQuestion[] = [
  {
    number: 1,
    label: "GDPR Compliance",
    question: "How do you handle GDPR compliance for EU customer data?",
  },
  {
    number: 2,
    label: "Prompt Injection",
    question: "What prevents prompt injection attacks on your RAG pipeline?",
  },
  {
    number: 3,
    label: "ERP Integration",
    question: "How long does an ERP integration take — SAP S/4HANA?",
  },
];

interface Props {
  isStreaming: boolean;
  customQuestion: string;
  onCustomChange: (v: string) => void;
  onAsk: (question: string) => void;
  pressedNumber: number | null;
}

function QuickFireButton({
  number,
  label,
  question,
  onClick,
  disabled,
  pressed,
}: {
  number: number;
  label: string;
  question: string;
  onClick: (q: string) => void;
  disabled: boolean;
  pressed: boolean;
}) {
  return (
    <button
      type="button"
      onClick={() => onClick(question)}
      disabled={disabled}
      className={`group flex flex-col items-start gap-1.5 rounded-sm border bg-bg-base px-4 py-3 text-left transition-all duration-150 hover:-translate-y-0.5 hover:border-accent/60 hover:bg-bg-elevated disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0 disabled:hover:border-border-subtle disabled:hover:bg-bg-base ${
        pressed ? "translate-y-px border-accent bg-bg-elevated" : "border-border-subtle"
      }`}
    >
      <div className="flex items-center gap-2">
        <span
          className={`flex h-5 w-5 items-center justify-center rounded-sm font-mono text-[10px] transition-colors ${
            pressed
              ? "bg-accent text-accent-foreground"
              : "bg-bg-elevated text-text-tertiary group-hover:bg-accent group-hover:text-accent-foreground"
          }`}
        >
          {number}
        </span>
        <span className="text-[13px] font-medium text-text-primary">{label}</span>
      </div>
      <span className="line-clamp-2 text-[12px] text-text-tertiary">{question}</span>
    </button>
  );
}

export function QuickFireBar({
  isStreaming,
  customQuestion,
  onCustomChange,
  onAsk,
  pressedNumber,
}: Props) {
  const handleSubmit = () => {
    const q = customQuestion.trim();
    if (q && !isStreaming) onAsk(q);
  };

  return (
    <div className="rounded-md border border-border-subtle bg-bg-surface p-4">
      <div className="mb-3 flex items-center justify-between">
        <span className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
          Quick-fire
        </span>
        <span className="font-mono text-[11px] text-text-tertiary">
          press 1, 2, or 3 · or type a custom question
        </span>
      </div>

      <div className="grid grid-cols-3 gap-3">
        {QUICK_FIRE.map((q) => (
          <QuickFireButton
            key={q.number}
            number={q.number}
            label={q.label}
            question={q.question}
            onClick={onAsk}
            disabled={isStreaming}
            pressed={pressedNumber === q.number}
          />
        ))}
      </div>

      <div className="mt-3 flex items-center gap-2">
        <input
          type="text"
          placeholder="Or ask anything…"
          value={customQuestion}
          onChange={(e) => onCustomChange(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              handleSubmit();
            }
          }}
          disabled={isStreaming}
          className="flex-1 rounded-sm border border-border-subtle bg-bg-base px-3 py-2 text-sm text-text-primary placeholder:text-text-tertiary transition-colors focus:border-accent focus:outline-none disabled:opacity-50"
        />
        <Button
          variant="default"
          size="sm"
          onClick={handleSubmit}
          disabled={!customQuestion.trim() || isStreaming}
        >
          Ask
          <ArrowRight className="ml-1.5 h-3 w-3" />
        </Button>
      </div>
    </div>
  );
}
