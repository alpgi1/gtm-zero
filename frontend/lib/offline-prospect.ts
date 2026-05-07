import type { ProspectSummaryDto } from "./types";

export const OFFLINE_PROSPECT: ProspectSummaryDto = {
  id: "offline-marie",
  fullName: "Marie Dubois",
  role: "VP of Engineering",
  companyName: "Lumeon Health",
  companyDomain: "lumeon.health",
  linkedinUrl: "https://linkedin.com/in/marie-dubois-lumeon",
  githubUrl: null,
  techStackSignals: ["Clinical AI", "FDA Submission", "Health"],
  outreachCount: 1,
  createdAt: new Date().toISOString(),
};

export const OFFLINE_PROSPECT_CONTEXT = "Just announced FDA submission";
