"use client";

interface CitationChipProps {
  marker: string;
  documentTitle: string;
  chunkIndex: number;
  onClick?: () => void;
}

export function CitationChip({
  marker,
  documentTitle,
  chunkIndex,
  onClick,
}: CitationChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-pill bg-bg-elevated border border-border-subtle hover:border-accent hover:bg-accent-muted/40 text-[11px] font-mono text-text-secondary hover:text-accent transition-colors duration-150"
    >
      <span className="text-accent">{marker}</span>
      <span className="text-text-secondary">{documentTitle}</span>
      <span className="text-text-tertiary">▸</span>
      <span className="text-text-tertiary">chunk_{chunkIndex}</span>
    </button>
  );
}
