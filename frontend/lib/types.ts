export type DashboardEventType =
  | "OBJECTION_ANSWERED"
  | "OUTREACH_GENERATED"
  | "OUTREACH_SENT_MOCK"
  | "DOCUMENT_INGESTED";

export interface DashboardCitation {
  marker: string;
  documentTitle: string;
  chunkIndex: number;
}

export interface DashboardActivityItem {
  id: string;
  eventType: DashboardEventType;
  summary: string;
  createdAt: string;
  citations: DashboardCitation[];
  /** True for offline-fallback rows. Backend rows always omit / set false. */
  synthetic?: boolean;
}

export interface DashboardMetrics {
  outreachSent: number;
  meetingsBooked: number;
  meetingsBookedDelta: number;
  pipelineCreatedEur: number;
}

export interface DashboardResponse {
  metrics: DashboardMetrics;
  activityFeed: DashboardActivityItem[];
}

// ── Prospect ──────────────────────────────────────────────────────────────

export interface ProspectSummaryDto {
  id: string;
  fullName: string | null;
  role: string | null;
  companyName: string;
  companyDomain: string | null;
  linkedinUrl: string | null;
  githubUrl: string | null;
  techStackSignals: string[];
  outreachCount: number;
  createdAt: string;
}

// ── Objection / Streaming ────────────────────────────────────────────────

export interface CitationDto {
  marker: string;
  chunkId: string;
  documentTitle: string;
  sourceType: string;
  chunkIndex: number;
  snippet: string;
}

export interface ObjectionResponseDto {
  queryId: string;
  question: string;
  answer: string;
  citations: CitationDto[];
  retrievedCount: number;
  firstTokenLatencyMs: number;
  totalLatencyMs: number;
  model: string;
}

export interface SessionObjectionRow {
  id: string;
  sessionId: string;
  question: string;
  answer: string;
  citationCount: number;
  firstTokenLatencyMs: number | null;
  totalLatencyMs: number | null;
  model: string | null;
  createdAt: string;
}

export interface ObjectionRequest {
  question: string;
  sessionId: string;
  topK?: number;
}

// SSE event payloads
export interface StartedEvent {
  queryId: string;
  timestamp: number;
}
export interface RetrievedEvent {
  citations: CitationDto[];
  latencyMs: number;
}
export interface FailedEvent {
  message: string;
  code: string;
}

// ── Outreach ─────────────────────────────────────────────────────────────

export type OutreachStatus = "GENERATED" | "APPROVED" | "SENT_MOCK";

export interface OutreachHistoryDto {
  id: string;
  prospectId: string;
  prospectFullName: string | null;
  prospectCompany: string;
  subject: string;
  bodyPreview: string;
  status: OutreachStatus;
  generationLatencyMs: number;
  createdAt: string;
}

export interface OutreachResponseDto {
  outreachId: string;
  prospectId: string;
  prospectFullName: string | null;
  prospectRole: string | null;
  prospectCompany: string;
  prospectCompanyDomain: string | null;
  prospectLinkedinUrl: string | null;
  usedSignals: string[];
  subject: string;
  body: string;
  personalizationBasis: string | null;
  model: string;
  generationPromptVersion: string;
  generationLatencyMs: number;
  status: OutreachStatus;
  createdAt: string;
}

export interface GenerateOutreachRequest {
  fullName?: string | null;
  role?: string | null;
  companyName: string;
  companyDomain?: string | null;
  linkedinUrl?: string | null;
  githubUrl?: string | null;
  techStackSignals?: string[];
  contextNotes?: string | null;
}

// ── Documents ────────────────────────────────────────────────────────────

export type DocumentSourceType =
  | "README"
  | "TECHNICAL_DOC"
  | "API_SPEC"
  | "ARCHITECTURE"
  | "LEGAL_CORPUS";

export interface DocumentListItem {
  id: string;
  title: string;
  sourceType: DocumentSourceType;
  charCount: number;
  chunkCount: number;
  ingestedAt: string;
  createdAt: string;
}

export interface DocumentChunkPreview {
  id: string;
  chunkIndex: number;
  tokenCount: number | null;
  snippet: string;
}

export interface DocumentDetail {
  id: string;
  title: string;
  sourceType: DocumentSourceType;
  sourcePath: string | null;
  charCount: number;
  chunkCount: number;
  ingestedAt: string;
  chunks: DocumentChunkPreview[];
}
