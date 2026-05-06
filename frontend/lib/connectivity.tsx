"use client";

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
} from "react";

type ConnectivityState = "connected" | "offline" | "unknown";

interface ConnectivityValue {
  state: ConnectivityState;
  setState: (s: ConnectivityState) => void;
}

const ConnectivityContext = createContext<ConnectivityValue | null>(null);

export function ConnectivityProvider({ children }: { children: React.ReactNode }) {
  const [state, setStateRaw] = useState<ConnectivityState>("unknown");
  const setState = useCallback((s: ConnectivityState) => setStateRaw(s), []);
  const value = useMemo(() => ({ state, setState }), [state, setState]);
  return (
    <ConnectivityContext.Provider value={value}>
      {children}
    </ConnectivityContext.Provider>
  );
}

export function useConnectivity(): ConnectivityValue {
  const ctx = useContext(ConnectivityContext);
  if (!ctx) {
    return { state: "unknown", setState: () => undefined };
  }
  return ctx;
}
