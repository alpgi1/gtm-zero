/**
 * Single fixed radial spotlight in the top-right corner. Static — no animation,
 * no scroll parallax. Adds 3-5% perceived depth without reading as decoration.
 */
export function RadialGlow() {
  return (
    <div
      aria-hidden
      className="pointer-events-none fixed top-0 right-0 z-0"
      style={{
        width: "720px",
        height: "720px",
        background:
          "radial-gradient(circle at top right, rgba(255, 176, 32, 0.06) 0%, rgba(255, 176, 32, 0.025) 30%, transparent 65%)",
      }}
    />
  );
}
