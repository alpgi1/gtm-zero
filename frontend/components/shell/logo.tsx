export function Logo({ collapsed = false }: { collapsed?: boolean }) {
  return (
    <div className="flex items-center gap-2.5">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden>
        <rect
          x="2"
          y="2"
          width="20"
          height="20"
          rx="3"
          stroke="var(--color-accent)"
          strokeWidth="1.5"
        />
        <rect x="16" y="2" width="6" height="6" rx="1" fill="var(--color-accent)" />
      </svg>
      {!collapsed && (
        <span className="text-[15px] font-medium text-text-primary tracking-tight">
          GTM-<span className="font-serif italic font-normal">Zero</span>
        </span>
      )}
    </div>
  );
}
