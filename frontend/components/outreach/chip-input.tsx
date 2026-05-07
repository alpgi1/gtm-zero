"use client";

import { useState, KeyboardEvent } from "react";
import { X } from "lucide-react";

interface ChipInputProps {
  value: string[];
  onChange: (next: string[]) => void;
  placeholder?: string;
}

export function ChipInput({ value, onChange, placeholder }: ChipInputProps) {
  const [draft, setDraft] = useState("");

  const commit = () => {
    const v = draft.trim();
    if (!v) return;
    if (value.includes(v)) {
      setDraft("");
      return;
    }
    onChange([...value, v]);
    setDraft("");
  };

  const remove = (chip: string) => {
    onChange(value.filter((c) => c !== chip));
  };

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      commit();
    } else if (e.key === "Backspace" && draft.length === 0 && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  };

  return (
    <div className="flex flex-wrap items-center gap-1.5 rounded-sm border border-border-subtle bg-bg-base px-2 py-1.5 focus-within:border-accent transition-colors">
      {value.map((chip) => (
        <span
          key={chip}
          className="inline-flex items-center gap-1 rounded-pill bg-bg-elevated border border-border-subtle px-2 py-0.5 text-[11px] text-text-secondary"
        >
          {chip}
          <button
            type="button"
            onClick={() => remove(chip)}
            className="text-text-tertiary hover:text-text-primary"
            aria-label={`Remove ${chip}`}
          >
            <X className="h-3 w-3" />
          </button>
        </span>
      ))}
      <input
        type="text"
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={handleKey}
        onBlur={commit}
        placeholder={value.length === 0 ? placeholder : ""}
        className="flex-1 min-w-[120px] bg-transparent border-none px-1 py-0.5 text-sm text-text-primary placeholder:text-text-tertiary focus:outline-none"
      />
    </div>
  );
}
