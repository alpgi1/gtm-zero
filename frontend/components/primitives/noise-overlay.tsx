/**
 * Single fixed full-bleed SVG noise layer that gives the dark surfaces a
 * matte, photographic feel. Pure decoration — pointer-events disabled,
 * z-index pinned below everything else.
 */
export function NoiseOverlay() {
  return (
    <div
      aria-hidden
      className="pointer-events-none fixed inset-0 z-0 opacity-[0.03] mix-blend-overlay"
    >
      <svg width="100%" height="100%">
        <filter id="gtm-noise">
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.9"
            numOctaves="2"
            stitchTiles="stitch"
          />
        </filter>
        <rect width="100%" height="100%" filter="url(#gtm-noise)" />
      </svg>
    </div>
  );
}
