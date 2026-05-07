import type {
  DashboardResponse,
  DocumentDetail,
  DocumentListItem,
  GenerateOutreachRequest,
  OutreachHistoryDto,
  OutreachResponseDto,
} from "./types";
import { getSyntheticDashboard } from "./synthetic";

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
    cache: "no-store",
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

// ── Dashboard ─────────────────────────────────────────────────────────────

export function fetchDashboard(): Promise<DashboardResponse> {
  return apiFetch<DashboardResponse>("/dashboard");
}

/**
 * Fetches the dashboard but silently falls back to a synthetic payload if
 * the backend is unreachable. Returns `{ data, online }` so the caller can
 * tag UI as synthetic without surfacing an error to the user.
 */
export async function fetchDashboardOrSynthetic(): Promise<{
  data: DashboardResponse;
  online: boolean;
}> {
  try {
    const data = await fetchDashboard();
    return { data, online: true };
  } catch (e) {
    console.warn("[gtm-zero] dashboard fetch failed, using synthetic:", e);
    return { data: getSyntheticDashboard(), online: false };
  }
}

// ── Outreach ──────────────────────────────────────────────────────────────

export function fetchRecentOutreach(
  limit = 20,
): Promise<OutreachHistoryDto[]> {
  return apiFetch<OutreachHistoryDto[]>(`/outreach/recent?limit=${limit}`);
}

export function fetchOutreach(id: string): Promise<OutreachResponseDto> {
  return apiFetch<OutreachResponseDto>(`/outreach/${id}`);
}

export function generateOutreach(
  payload: GenerateOutreachRequest,
): Promise<OutreachResponseDto> {
  return apiFetch<OutreachResponseDto>("/outreach/generate", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function markOutreachSent(id: string): Promise<OutreachResponseDto> {
  return apiFetch<OutreachResponseDto>(`/outreach/${id}/send-mock`, {
    method: "POST",
  });
}

// ── Documents ─────────────────────────────────────────────────────────────

export function fetchDocuments(): Promise<DocumentListItem[]> {
  return apiFetch<DocumentListItem[]>("/admin/documents");
}

export function fetchDocumentDetail(id: string): Promise<DocumentDetail> {
  return apiFetch<DocumentDetail>(`/admin/documents/${id}`);
}

export function reseedDocuments(force = true): Promise<unknown> {
  return apiFetch(`/admin/documents/seed?force=${force}`, { method: "POST" });
}
