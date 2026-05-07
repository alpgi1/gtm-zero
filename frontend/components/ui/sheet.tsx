"use client";

import * as React from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import { cn } from "@/lib/cn";

interface SheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
}

interface SheetContentProps {
  side?: "right";
  className?: string;
  children: React.ReactNode;
  onClose: () => void;
}

/**
 * Minimal slide-over panel. Fixed to the right edge, click-outside + ESC to dismiss.
 * Renders into a portal so the parent layout (RadialGlow, fixed sidebar) doesn't
 * fight z-index. Animation is opacity + translate-x driven by CSS transitions.
 */
export function Sheet({ open, onOpenChange, children }: SheetProps) {
  const [mounted, setMounted] = React.useState(false);
  const [visible, setVisible] = React.useState(false);

  React.useEffect(() => {
    if (open) {
      setMounted(true);
      const id = window.requestAnimationFrame(() => setVisible(true));
      return () => window.cancelAnimationFrame(id);
    }
    setVisible(false);
    const t = window.setTimeout(() => setMounted(false), 220);
    return () => window.clearTimeout(t);
  }, [open]);

  React.useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onOpenChange(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onOpenChange]);

  React.useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  if (!mounted || typeof document === "undefined") return null;

  const close = () => onOpenChange(false);

  return createPortal(
    <div className="fixed inset-0 z-50">
      <div
        aria-hidden
        onClick={close}
        className={cn(
          "absolute inset-0 bg-bg-base/70 transition-opacity duration-200",
          visible ? "opacity-100" : "opacity-0",
        )}
      />
      <SheetContext.Provider value={{ visible, close }}>
        {React.Children.map(children, (child) => child)}
      </SheetContext.Provider>
    </div>,
    document.body,
  );
}

interface SheetContextValue {
  visible: boolean;
  close: () => void;
}
const SheetContext = React.createContext<SheetContextValue | null>(null);

export function SheetContent({
  className,
  children,
  side = "right",
}: Omit<SheetContentProps, "onClose">) {
  const ctx = React.useContext(SheetContext);
  if (!ctx) return null;
  const { visible, close } = ctx;

  const sideClasses =
    side === "right"
      ? "right-0 top-0 h-screen border-l"
      : "right-0 top-0 h-screen border-l";
  const transformClass = visible ? "translate-x-0" : "translate-x-full";

  return (
    <div
      role="dialog"
      aria-modal="true"
      className={cn(
        "absolute flex flex-col bg-bg-surface border-border-subtle shadow-2xl transition-transform duration-200 ease-[cubic-bezier(0.25,1,0.5,1)]",
        sideClasses,
        transformClass,
        className,
      )}
    >
      <button
        type="button"
        onClick={close}
        aria-label="Close"
        className="absolute right-3 top-3 flex h-7 w-7 items-center justify-center rounded-sm text-text-tertiary hover:bg-bg-elevated hover:text-text-primary transition-colors"
      >
        <X className="h-3.5 w-3.5" strokeWidth={1.75} />
      </button>
      {children}
    </div>
  );
}

export function SheetHeader({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div
      className={cn(
        "border-b border-border-subtle px-5 pb-4 pt-5 pr-12",
        className,
      )}
    >
      {children}
    </div>
  );
}

export function SheetTitle({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <h2
      className={cn(
        "text-lg font-semibold tracking-tight text-text-primary",
        className,
      )}
    >
      {children}
    </h2>
  );
}

export function SheetDescription({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <p className={cn("mt-1 text-sm text-text-secondary", className)}>
      {children}
    </p>
  );
}

export function SheetFooter({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div
      className={cn(
        "mt-auto border-t border-border-subtle px-5 py-4",
        className,
      )}
    >
      {children}
    </div>
  );
}
