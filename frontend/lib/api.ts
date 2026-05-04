const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...init?.headers },
    ...init,
  });

  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new ApiError(res.status, text);
  }

  return res.json() as Promise<T>;
}

// ── Health ────────────────────────────────────────────────────────────────

export interface HealthResponse {
  status: "UP" | "DOWN";
  service: string;
  timestamp: string;
  db: "UP" | "DOWN";
}

export function checkHealth(): Promise<HealthResponse> {
  return apiFetch<HealthResponse>("/health");
}
