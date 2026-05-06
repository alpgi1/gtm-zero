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
