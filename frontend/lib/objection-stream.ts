import type {
  CitationDto,
  FailedEvent,
  ObjectionRequest,
  ObjectionResponseDto,
  RetrievedEvent,
  StartedEvent,
} from "./types";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

export interface StreamHandlers {
  onStarted?: (e: StartedEvent) => void;
  onRetrieved?: (e: RetrievedEvent) => void;
  onToken?: (text: string) => void;
  onCompleted?: (response: ObjectionResponseDto) => void;
  onFailed?: (e: FailedEvent) => void;
}

/**
 * POST + streaming-body SSE consumer. EventSource doesn't support POST,
 * so we use fetch with a ReadableStream reader and parse SSE frames manually.
 */
export async function streamObjection(
  request: ObjectionRequest,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const res = await fetch(`${API_BASE}/objections/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    },
    body: JSON.stringify(request),
    signal,
  });

  if (!res.ok || !res.body) {
    throw new Error(`Stream failed: ${res.status}`);
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    let idx;
    while ((idx = buffer.indexOf("\n\n")) !== -1) {
      const rawEvent = buffer.slice(0, idx);
      buffer = buffer.slice(idx + 2);
      dispatch(rawEvent, handlers);
    }
  }

  // flush
  if (buffer.trim()) dispatch(buffer, handlers);
}

function dispatch(rawEvent: string, h: StreamHandlers) {
  // SSE frames: "event: name\ndata: {json}" — but `data:` lines may repeat.
  let eventName: string | null = null;
  const dataParts: string[] = [];
  for (const line of rawEvent.split("\n")) {
    if (line.startsWith("event:")) eventName = line.slice(6).trim();
    else if (line.startsWith("data:")) dataParts.push(line.slice(5).trim());
  }
  if (!eventName || dataParts.length === 0) return;
  const dataStr = dataParts.join("\n");

  let payload: unknown;
  try {
    payload = JSON.parse(dataStr);
  } catch {
    return;
  }

  switch (eventName) {
    case "started":
      h.onStarted?.(payload as StartedEvent);
      break;
    case "retrieved": {
      const p = payload as { citations: CitationDto[]; latencyMs: number };
      h.onRetrieved?.(p);
      break;
    }
    case "token": {
      const p = payload as { text: string };
      h.onToken?.(p.text);
      break;
    }
    case "completed": {
      // Backend wraps the response in Completed{response: ObjectionResponse}
      const p = payload as { response?: ObjectionResponseDto } & ObjectionResponseDto;
      const resolved = p.response ?? (p as ObjectionResponseDto);
      h.onCompleted?.(resolved);
      break;
    }
    case "failed":
      h.onFailed?.(payload as FailedEvent);
      break;
  }
}

export async function fetchProspectByLinkedinUrl(
  url: string,
  signal?: AbortSignal,
): Promise<import("./types").ProspectSummaryDto | null> {
  const res = await fetch(
    `${API_BASE}/prospects?linkedinUrl=${encodeURIComponent(url)}`,
    { cache: "no-store", signal },
  );
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`prospects lookup ${res.status}`);
  return (await res.json()) as import("./types").ProspectSummaryDto;
}

export async function fetchSessionHistory(
  sessionId: string,
  signal?: AbortSignal,
): Promise<import("./types").SessionObjectionRow[]> {
  const res = await fetch(`${API_BASE}/objections/session/${sessionId}`, {
    cache: "no-store",
    signal,
  });
  if (!res.ok) throw new Error(`session history ${res.status}`);
  return (await res.json()) as import("./types").SessionObjectionRow[];
}
