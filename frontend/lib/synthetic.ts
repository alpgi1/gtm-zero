import type { DashboardActivityItem, DashboardResponse } from "./types";

/**
 * Realistic-looking activity rows used only when the backend is unreachable.
 * Every row is tagged `synthetic: true` so the UI can label them honestly.
 * The pitch needs the dashboard to look alive even on a flaky conference Wi-Fi.
 */
export function getSyntheticActivityFeed(): DashboardActivityItem[] {
  const now = Date.now();
  const min = (n: number) => new Date(now - n * 60_000).toISOString();
  return [
    {
      id: "syn-1",
      eventType: "OBJECTION_ANSWERED",
      summary: "Answered: How do you handle GDPR compliance for EU customer data?",
      createdAt: min(2),
      citations: [
        { marker: "[1]", documentTitle: "regu_data_handling.md", chunkIndex: 2 },
        { marker: "[2]", documentTitle: "regu_security.md", chunkIndex: 1 },
      ],
      synthetic: true,
    },
    {
      id: "syn-2",
      eventType: "OUTREACH_SENT_MOCK",
      summary: "Sent outreach to Marie Dubois at Lumeon Health",
      createdAt: min(7),
      citations: [],
      synthetic: true,
    },
    {
      id: "syn-3",
      eventType: "OBJECTION_ANSWERED",
      summary: "Answered: What prevents prompt injection attacks?",
      createdAt: min(14),
      citations: [
        { marker: "[1]", documentTitle: "regu_security.md", chunkIndex: 0 },
        { marker: "[2]", documentTitle: "regu_architecture.md", chunkIndex: 3 },
      ],
      synthetic: true,
    },
    {
      id: "syn-4",
      eventType: "OUTREACH_GENERATED",
      summary: "Generated outreach to Tomasz Krawczyk at DataNova GmbH",
      createdAt: min(28),
      citations: [],
      synthetic: true,
    },
    {
      id: "syn-5",
      eventType: "OBJECTION_ANSWERED",
      summary: "Answered: How long does a typical ERP integration take?",
      createdAt: min(43),
      citations: [
        { marker: "[1]", documentTitle: "regu_integrations.md", chunkIndex: 1 },
      ],
      synthetic: true,
    },
  ];
}

export function getSyntheticDashboard(): DashboardResponse {
  return {
    metrics: {
      outreachSent: 127,
      meetingsBooked: 14,
      meetingsBookedDelta: 27,
      pipelineCreatedEur: 1_200_000,
    },
    activityFeed: getSyntheticActivityFeed(),
  };
}
