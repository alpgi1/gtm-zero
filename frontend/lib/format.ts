const integerFormatter = new Intl.NumberFormat("de-DE", {
  maximumFractionDigits: 0,
});

export function formatInteger(value: number): string {
  return integerFormatter.format(Math.round(value));
}

/**
 * Compact EUR formatter for the dashboard hero — keeps "1.2M €" readable
 * at 40px without crowding. Falls back to grouping for sub-million values.
 */
export function formatCurrencyEur(value: number): string {
  const v = Math.round(value);
  if (v >= 1_000_000) {
    const m = v / 1_000_000;
    const trimmed = m % 1 === 0 ? m.toFixed(0) : m.toFixed(1);
    return `${trimmed}M €`;
  }
  if (v >= 1_000) {
    const k = v / 1_000;
    const trimmed = k % 1 === 0 ? k.toFixed(0) : k.toFixed(1);
    return `${trimmed}K €`;
  }
  return `${integerFormatter.format(v)} €`;
}

/**
 * Coarse relative time used by the activity feed. We don't need a full
 * library for the demo — minutes/hours/days is enough.
 */
export function formatRelativeTime(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return "";
  const diffSec = Math.max(1, Math.round((Date.now() - then) / 1000));
  if (diffSec < 60) return `${diffSec}s ago`;
  const min = Math.round(diffSec / 60);
  if (min < 60) return `${min} ${min === 1 ? "minute" : "minutes"} ago`;
  const hr = Math.round(min / 60);
  if (hr < 24) return `${hr} ${hr === 1 ? "hour" : "hours"} ago`;
  const day = Math.round(hr / 24);
  return `${day} ${day === 1 ? "day" : "days"} ago`;
}
