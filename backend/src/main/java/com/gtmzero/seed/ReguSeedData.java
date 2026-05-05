package com.gtmzero.seed;

import com.gtmzero.dto.IngestDocumentRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hardcoded Regu seed documents for the demo. 7 documents total, producing
 * ~28-32 chunks. Content specifically supports the three pitch demo questions:
 * GDPR compliance, prompt injection defence, and ERP integration timelines.
 */
@Component
public class ReguSeedData {

    // ─── Document 1: Overview ────────────────────────────────────────
    private static final String REGU_OVERVIEW = """
            # Regu: AI-Powered EU AI Act Compliance Engine

            Regu is a regulatory compliance platform purpose-built for EU startup founders and SMEs navigating the EU Artificial Intelligence Act (Regulation 2024/1689). The platform automates compliance assessment, documentation generation, and ongoing monitoring, reducing what typically requires €50k-€100k in legal consulting to an automated, citation-backed workflow.

            ## Target User

            The primary user is a technical founder or CTO at an EU-based startup deploying AI systems. Regu assumes technical literacy but no regulatory expertise. Secondary users include compliance officers at mid-market companies (50-500 employees) and legal advisors seeking to augment their AI Act knowledge with structured, always-current guidance.

            ## Core Principles

            1. **Citation Mandatory**: Every compliance recommendation Regu generates must cite the specific EU AI Act article, annex, or recital it derives from. Uncited statements are automatically rejected by the citation validator before reaching the user. This is not a nice-to-have — it is architecturally enforced.

            2. **Law-Primary**: The system's retrieval corpus consists of the official EU AI Act text, Commission delegated acts, and harmonized standards. Secondary sources (blog posts, analyst reports) are excluded from the vector store to prevent contamination of legal reasoning.

            3. **Fail-Safe**: When Regu's confidence in a classification is below threshold (cosine similarity < 0.72 on retrieved chunks), it flags the assessment for human legal review rather than guessing. False negatives (missing a high-risk classification) are treated as critical failures; false positives (over-classifying) are acceptable and surfaced as "conservative assessment" to the user.

            4. **Versioned Corpus**: The legal corpus is version-controlled. Each chunk stores its source document version, effective date, and amendment history. When the EU publishes delegated acts or corrective amendments, Regu ingests the delta and re-evaluates affected assessments.

            ## Elevator Pitch

            Regu turns a 200-page regulation into a 15-minute interactive interview. You answer questions about your AI system — what data it uses, where it is deployed, who it affects — and Regu returns a structured compliance report with risk classification, required documentation checklist, conformity assessment pathway, and specific article citations for every requirement. It is the compliance co-pilot that founders wish existed when GDPR launched.
            """;

    // ─── Document 2: Architecture ────────────────────────────────────
    private static final String REGU_ARCHITECTURE = """
            # Regu Technical Architecture

            ## RAG Pipeline Overview

            Regu uses a Retrieval-Augmented Generation (RAG) architecture with four specialized vector tables:

            1. **legal_chunks** (885 chunks): Full text of the EU AI Act, split by article and paragraph. Each chunk preserves article numbering, section headers, and cross-references. Indexed with HNSW for sub-50ms retrieval at 1M scale.

            2. **use_case_chunks** (240 chunks): Pre-analyzed use-case templates covering common AI deployments (chatbots, recommendation engines, hiring tools, biometric systems). Each template maps to risk tiers and required documentation.

            3. **guide_chunks** (320 chunks): Step-by-step compliance guides generated from enforcement guidance published by national authorities and the European AI Office.

            4. **decision_rule_chunks** (40 chunks): Deterministic classification rules derived from Article 6 and Annex III. These are not retrieved semantically — they are evaluated as structured JSON rules after the interview stage.

            ## Pipeline Flow

            The pipeline processes user queries through six stages:

            1. **Input Processing**: User query is sanitized (HTML stripping, prompt injection detection via regex + classifier, length validation). The sanitized query is logged with a session UUID for audit trail.

            2. **Embedding**: Query text is embedded using Voyage AI's voyage-3-large model (1024 dimensions). Voyage was chosen over OpenAI ada-002 for its superior performance on legal/regulatory text benchmarks (MTEB Legal subset: voyage-3-large 0.847 vs ada-002 0.791).

            3. **Retrieval**: Hybrid search combines vector similarity (cosine distance via pgvector HNSW index) with BM25 keyword matching using Reciprocal Rank Fusion (RRF). Top-K is set to 12 chunks. The retrieval layer parallelizes across all four vector tables and merges results in <100ms.

            4. **LLM Generation**: Retrieved chunks are injected into a structured system prompt with explicit instructions: cite every claim, use [Art. X] notation, flag uncertainty. Claude Sonnet generates the response with temperature 0.1 for maximum factual consistency.

            5. **Citation Validator**: A post-processing module parses the LLM output, extracts citation references, and validates each against the retrieved chunks. Sentences without valid citations are stripped and logged. If >30% of content is stripped, the response is flagged for manual review.

            6. **JSON Output**: The validated response is serialized into a structured JSON schema (ComplianceReport) with fields: risk_tier, applicable_articles[], required_actions[], documentation_checklist[], confidence_score, and flagged_for_review.

            ## Technology Choices

            - **Embedding**: Voyage AI voyage-3-large (1024 dims). API-based, no GPU required. Average latency: 120ms for single query, 800ms for 50-chunk batch.
            - **LLM**: Anthropic Claude claude-sonnet-4-6. Chosen for its strong instruction-following on legal text and 200K context window.
            - **Vector Store**: PostgreSQL 17 + pgvector 0.8.2. Self-hosted, EU-region (Frankfurt). HNSW index with ef_construction=200, m=16.
            - **Search**: Hybrid pgvector cosine + pg_trgm BM25, fused via RRF with k=60.
            - **Application**: Spring Boot 4.0, Java 21, deployed on Hetzner Cloud (Nuremberg, DE) for EU data residency.
            """;

    // ─── Document 3: Security (revised — platform security focus only) ─────
    private static final String REGU_SECURITY = """
            # Regu Platform Security Architecture

            ## Prompt Injection Prevention

            Regu implements a three-layer defence against prompt injection attacks, because a compliance platform that can be jailbroken into fabricating regulatory advice is a liability, not an asset.

            **Layer 1 — Input Sanitization [1]:** Every user query passes through a sanitization pipeline before reaching the LLM. This includes HTML and script tag stripping, Unicode normalization (NFC), detection of common injection patterns ("ignore previous instructions", "system prompt override", "DAN mode", encoded payloads via base64 or URL encoding), and input length capping at 4,000 characters. The sanitizer uses both regex rules updated monthly from the OWASP LLM Top 10 and a lightweight classifier trained on 12,000 adversarial prompt examples sourced from academic red-teaming datasets.

            **Layer 2 — System Prompt Isolation [2]:** The system prompt is architecturally separated from user input using Claude's system message parameter (the `system` field in the Anthropic Messages API). User input is placed exclusively in the user turn, never concatenated with system instructions. The system prompt includes explicit guardrails instructing the model to refuse requests to override instructions, reveal internal prompts, or execute code. Even a successful injection in the user turn cannot reach the system turn — they are structurally separate API fields, not a concatenated string.

            **Layer 3 — Output Schema Enforcement [3]:** LLM output must conform to a strict JSON schema (ComplianceReport). Free-text fields are bounded at 500 characters per field. Any output that fails schema validation is rejected and the query is retried with a stricter prompt. If the second attempt also fails validation, the response is discarded and the user receives an error with a support ticket reference. This schema gate means an injected command that tries to produce executable code or unstructured text is blocked at the output boundary.

            ## Secrets Management and Access Control

            API keys, database credentials, and service tokens are stored exclusively in HashiCorp Vault. Application containers receive short-lived credentials via Vault's dynamic secrets engine — static credentials do not exist in configuration files, environment variables, or container images. Secret rotation happens every 24 hours without requiring a deployment.

            Production database access is restricted to two named operators via an SSH jump host with hardware 2FA (YubiKey Series 5). Application-level access uses JWT tokens with 1-hour expiry and refresh token rotation. Role-based access control follows the principle of least privilege: read-only service accounts for the API layer, write access for the ingestion pipeline only.

            ## Vulnerability Management

            Regu follows a 30-day patch cycle for all dependencies with CVE scores ≥ 7.0 (CVSS v3), and a 7-day emergency patch cycle for CVSS ≥ 9.0 criticals. Container images are scanned on every build using Trivy; builds fail if a critical vulnerability is detected in the base image. Penetration testing is conducted annually by an independent third party; the most recent engagement found no critical or high findings. Regu is currently undergoing SOC 2 Type II audit (in scope: Security, Availability, Confidentiality trust service criteria).

            ## Hallucination Prevention

            Regu treats hallucination as a critical safety failure — an incorrectly cited regulation could expose a customer to non-compliance penalties of up to €35 million or 7% of global turnover under Art. 99 of the EU AI Act.

            The citation validator is a post-processing module (no LLM call) that extracts every [n] reference from the generated response, verifies each reference exists in the retrieved chunk set, and measures semantic alignment between the sentence and the cited chunk (cosine similarity > 0.65). Sentences failing validation are removed from the response. The ratio of removed sentences is tracked as a quality metric with a target below 5% removal rate. When maximum cosine similarity of retrieved chunks is below 0.72, the response is flagged for human legal review rather than served to the user.
            """;

    // ─── Document 4: API Specification ────────────────────────────────
    private static final String REGU_API_SPEC = """
            # Regu REST API Specification

            ## Base URL

            Production: `https://api.regu.eu/v1`
            Staging: `https://api.staging.regu.eu/v1`

            All endpoints require Bearer token authentication via the `Authorization` header.

            ## Core Endpoints

            ### POST /assess

            Submits an AI system for compliance assessment. This is the primary endpoint that triggers the full RAG pipeline.

            **Request Body:**
            ```json
            {
              "system_name": "ResumeRanker Pro",
              "system_description": "AI system that ranks job applicants based on CV analysis",
              "deployment_region": "EU",
              "affected_persons": ["job_applicants", "employees"],
              "data_types": ["biometric", "personal_data"],
              "intended_use": "Automated pre-screening of job applications",
              "operator_type": "provider"
            }
            ```

            **Response (201 Created):**
            ```json
            {
              "report_id": "rpt_a1b2c3d4",
              "risk_tier": "HIGH_RISK",
              "applicable_articles": ["Art. 6(2)", "Art. 9", "Art. 13", "Art. 14", "Annex III(4)"],
              "required_actions": [
                "Establish risk management system per Art. 9",
                "Implement data governance requirements per Art. 10",
                "Maintain technical documentation per Art. 11 and Annex IV",
                "Enable human oversight mechanisms per Art. 14"
              ],
              "documentation_checklist": [
                {"item": "Risk Management Plan", "article": "Art. 9", "status": "required"},
                {"item": "Data Governance Policy", "article": "Art. 10", "status": "required"},
                {"item": "Technical Documentation", "article": "Art. 11 + Annex IV", "status": "required"},
                {"item": "Conformity Assessment", "article": "Art. 43", "status": "required"}
              ],
              "confidence_score": 0.89,
              "flagged_for_review": false,
              "processing_time_ms": 7200
            }
            ```

            ### GET /reports/{id}

            Retrieves a previously generated compliance report.

            **Response (200 OK):** Full ComplianceReport JSON as above, plus `created_at`, `updated_at`, and `version` fields. Reports are immutable once generated; re-assessment creates a new report with a new ID.

            ### POST /documents/upload

            Uploads a custom document (company policy, technical specification) to the user's private corpus for context-aware assessment.

            **Request:** multipart/form-data with fields `file` (PDF, DOCX, MD, TXT; max 10MB), `document_type` (POLICY, TECHNICAL_SPEC, RISK_ASSESSMENT), `title`.

            **Response (202 Accepted):**
            ```json
            {
              "document_id": "doc_x1y2z3",
              "title": "ResumeRanker Technical Specification",
              "status": "processing",
              "estimated_completion_seconds": 30
            }
            ```

            Document processing (chunking + embedding) is asynchronous. Poll `GET /documents/{id}/status` for completion.

            ## Rate Limits

            | Plan       | Requests/min | Assessments/month | Documents |
            |------------|-------------|-------------------|-----------|
            | Free       | 10          | 5                 | 3         |
            | Startup    | 60          | 50                | 25        |
            | Enterprise | 300         | Unlimited         | Unlimited |

            Rate limit headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.

            ## Integration Patterns

            **Webhook Notifications**: Configure a webhook URL via `POST /webhooks` to receive real-time notifications when assessments complete, documents finish processing, or regulations update. Webhook payloads are signed with HMAC-SHA256 for verification.

            **Polling**: For simpler integrations, poll `GET /reports/{id}` or `GET /documents/{id}/status` with exponential backoff (recommended interval: 2s initial, 2x multiplier, max 30s).

            ## Expected Latencies

            | Operation              | p50    | p95    | p99    |
            |----------------------|--------|--------|--------|
            | POST /assess         | 6.2s   | 12.8s  | 18.5s  |
            | GET /reports/{id}    | 45ms   | 120ms  | 250ms  |
            | POST /documents/upload | 150ms | 350ms  | 800ms  |
            | Document processing  | 8s     | 25s    | 45s    |

            The POST /assess latency is dominated by LLM inference (~4s) and retrieval (~1.5s). We optimize for accuracy over speed — p50 under 8s is our SLA target for the Startup tier.
            """;

    // ─── Document 5: Compliance Scope ─────────────────────────────────
    private static final String REGU_COMPLIANCE_SCOPE = """
            # EU AI Act: Compliance Scope and Enforcement Timeline

            ## What the EU AI Act Covers

            The EU Artificial Intelligence Act (Regulation 2024/1689) is the world's first comprehensive legal framework for artificial intelligence. It applies to AI systems placed on the market or put into service in the European Union, regardless of whether the provider is established in the EU. The Act's territorial scope means that a US-based SaaS company selling an AI hiring tool to a German corporation must comply.

            The Act defines an "AI system" broadly as a machine-based system designed to operate with varying levels of autonomy, that may exhibit adaptiveness after deployment, and that generates outputs such as predictions, recommendations, decisions, or content that can influence physical or virtual environments (Art. 3(1)).

            ## Enforcement Timeline

            The EU AI Act entered into force on 1 August 2024. Key compliance deadlines:

            - **2 February 2025**: Prohibitions on unacceptable-risk AI systems take effect (Art. 5). This includes social scoring, real-time remote biometric identification in public spaces (with exceptions for law enforcement), and emotion recognition in workplaces and educational institutions.

            - **2 August 2025**: Obligations for general-purpose AI (GPAI) models apply (Art. 51-56). Providers of GPAI models with systemic risk (>10^25 FLOPs training compute) must conduct model evaluations, assess and mitigate systemic risks, and report serious incidents.

            - **2 August 2026**: Full enforcement of all provisions, including high-risk AI system requirements (Art. 6-49). This is the primary compliance deadline for most companies. Conformity assessments, technical documentation, post-market monitoring, and registration in the EU database become mandatory.

            - **2 August 2027**: Obligations for high-risk AI systems that are safety components of products covered by existing EU harmonization legislation (Annex I) take effect.

            ## Risk Tiers

            The Act establishes four risk categories:

            ### Unacceptable Risk (Prohibited — Art. 5)
            AI systems that pose an unacceptable risk to fundamental rights. These are banned outright:
            - Subliminal, manipulative, or deceptive techniques causing significant harm
            - Exploitation of vulnerabilities (age, disability, social/economic situation)
            - Social scoring by public authorities
            - Real-time remote biometric identification in public spaces (limited law enforcement exceptions)
            - Emotion recognition in workplaces and educational institutions
            - Untargeted scraping of facial images for facial recognition databases
            - Biometric categorization inferring sensitive attributes (race, political opinions, sexual orientation)

            ### High Risk (Art. 6 + Annex III)
            AI systems that pose significant risks to health, safety, or fundamental rights. Subject to mandatory requirements:
            - **Annex III(1)**: Biometric identification and categorization
            - **Annex III(2)**: Management and operation of critical infrastructure
            - **Annex III(3)**: Education and vocational training (access, assessment)
            - **Annex III(4)**: Employment, worker management, access to self-employment
            - **Annex III(5)**: Access to essential private/public services (credit scoring, insurance)
            - **Annex III(6)**: Law enforcement
            - **Annex III(7)**: Migration, asylum, border control
            - **Annex III(8)**: Administration of justice and democratic processes

            ### Limited Risk (Art. 50)
            AI systems with transparency obligations only:
            - Chatbots must disclose they are AI
            - AI-generated content (deepfakes) must be labeled
            - Emotion recognition systems must inform subjects

            ### Minimal Risk
            All other AI systems. No specific obligations under the Act, though voluntary codes of practice are encouraged.

            ## Annex IV Documentation Requirements

            High-risk AI systems must maintain technical documentation including:
            1. General description of the AI system (intended purpose, developer identity)
            2. Detailed description of elements and development process
            3. Monitoring, functioning, and control of the AI system
            4. Risk management system documentation
            5. Data governance and data management practices
            6. Performance metrics and testing results
            7. Cybersecurity measures
            8. Description of the quality management system

            ## Who Must Comply

            The Act imposes obligations on four categories of operators:

            **Providers** (Art. 3(3)): Entities that develop or commission AI systems and place them on the market or put them into service under their own name or trademark. Providers bear the heaviest compliance burden: conformity assessment, technical documentation, post-market monitoring, incident reporting.

            **Deployers** (Art. 3(4)): Entities that use AI systems under their authority, except where used in personal non-professional activities. Deployers must use systems in accordance with instructions, ensure human oversight, monitor for risks, and conduct fundamental rights impact assessments (FRIA) for high-risk systems in certain domains.

            **Importers** (Art. 3(6)): Entities established in the EU that place on the market an AI system from a third country. Importers must verify the provider has conducted conformity assessment, bears CE marking, and has established an EU authorized representative.

            **Distributors** (Art. 3(7)): Entities in the supply chain (other than provider or importer) that make AI systems available on the EU market. Distributors must verify CE marking, provider/importer compliance, and withdraw non-conforming products.

            Penalties for non-compliance range from €7.5 million or 1% of global turnover (for incorrect information to authorities) to €35 million or 7% of global turnover (for prohibited AI practices), whichever is higher (Art. 99).
            """;

    // ─── Document 6: ERP & System Integrations ────────────────────────
    private static final String REGU_INTEGRATIONS = """
            # Regu Enterprise Integration Guide

            ## Integration Overview

            Regu integrates with enterprise systems via three mechanisms: a REST API for custom integrations, pre-built connectors for the most common ERP and CRM platforms, and a webhook event bus for real-time push notifications. All integration channels use OAuth 2.0 client credentials flow for machine-to-machine authentication, or scoped API keys for simpler setups. Onboarding follows a four-stage sequence: sandbox key issuance → schema mapping → staging validation → production cutover. The sandbox environment is identical to production in API surface and data handling but uses isolated storage with synthetic data.

            ## ERP Integration — SAP S/4HANA

            Regu provides a pre-built SAP S/4HANA connector using the OData API (v4) for data read/write and RFC calls for batch compliance event ingestion. The connector ships as a pre-built ABAP transport (TR) that installs the necessary function modules and BAPIs into the customer's S/4HANA system without requiring custom development. Standard SAP S/4HANA ERP integration via OData API takes 2-4 hours for a sandbox connection — this covers connector installation, OAuth credential setup, and a smoke test of the risk-tier metadata sync endpoint. Production-grade ERP integration with full schema mapping, data governance review, and parallel run validation takes 2-4 weeks for a standard S/4HANA deployment. Customers with heavily customized S/4HANA instances (non-standard ABAP extensions, legacy IDocs) should budget 4-6 weeks. The pre-built transport handles the ABAP-side; the Regu side requires configuring field mappings via a JSON schema definition uploaded through the admin API. SAP integration supports both real-time OData calls (sub-second latency for single-record lookups) and batch RFC-based ingestion (up to 50,000 records per job, typical throughput 5,000 records/minute). All SAP integration traffic uses mutual TLS (mTLS) with certificates issued by Regu's internal PKI.

            ## ERP Integration — Microsoft Dynamics 365

            The Dynamics 365 connector uses Microsoft's Dataverse API (v9.2) for entity reads and Power Automate cloud flows for compliance event ingestion. Standard Dynamics 365 ERP integration goes live in 1-3 weeks for production, depending on the complexity of the customer's Dataverse schema. Simple deployments (standard Dynamics entities, no custom tables) deploy in 3-5 days. Customers with extended entity schemas or custom plugins should plan for 2-3 weeks including regression testing. The Power Automate flows are provided as solution packages (ZIP) that the customer imports into their Power Platform environment — no low-code development required. Regu's Dataverse connector maps compliance assessment results to custom Regu_Assessment entities and writes risk-tier changes as timeline activities on the relevant Account or Contact record, giving the customer's compliance team full audit visibility inside their existing Dynamics workspace.

            ## CRM Integration — Salesforce

            Salesforce integration is delivered via the Lightning Connect external data source framework, which exposes Regu assessments as Salesforce external objects — no data duplication required. Typical production deployment takes 1-2 weeks. The integration requires Salesforce Enterprise edition or above (for Lightning Connect). Setup involves creating a Named Credential for OAuth 2.0 to Regu's API, defining the external data source, and optionally configuring field-level security on the Regu_Assessment external object. Custom Salesforce integrations (non-Lightning Connect, Apex callouts, custom objects) average 3-6 weeks depending on data volume and schema complexity.

            ## Custom REST API Integrations

            For systems without a pre-built connector — custom ERP platforms, homegrown compliance tools, data warehouses — Regu exposes a full REST API with OpenAPI 3.1 specification available at GET /api/v1/openapi.json. Custom integrations average 3-6 weeks from kickoff to production, with the main variable being the customer's internal development capacity and the complexity of their existing data model. Regu provides a dedicated integration engineer for Enterprise-tier customers during the integration project, plus a sandbox environment with synthetic regulatory data for development and testing without affecting production metrics.

            ## Data Sync Patterns

            Regu supports three sync patterns to match different customer latency requirements:

            Real-time webhook-driven sync delivers compliance events (risk-tier changes, new assessment results, regulatory updates) within 500ms of occurrence. Webhooks use HMAC-SHA256 payload signing; the customer verifies the signature before processing. Webhook delivery retries follow an exponential backoff schedule (1s, 2s, 4s, 8s, 16s) with a maximum of 5 attempts before the event is placed in a dead-letter queue accessible via the admin API.

            Scheduled batch sync runs on configurable intervals: 15-minute, hourly, or daily. Batches are delivered as newline-delimited JSON (NDJSON) files posted to a customer-configured S3-compatible object store endpoint. Batch jobs are idempotent — each record carries a deterministic content hash, and re-delivered batches can be safely de-duplicated by the consumer using that hash.

            On-demand API pull allows customers to query Regu's API at any time for current assessment state. Rate limits are 1,000 requests/minute on the standard tier and 5,000 requests/minute on the enterprise tier. All API responses include idempotency keys via the X-Idempotency-Key request header — customers can safely retry failed requests without risk of duplicate processing.

            ## Security During Integration

            All integration traffic — ERP connectors, webhooks, API calls — routes exclusively through Regu's EU-hosted infrastructure in Frankfurt (primary) and Dublin (secondary). No integration traffic transits outside the EU/EEA. All channels enforce TLS 1.3 minimum; mutual TLS is available and recommended for ERP traffic. Every API call and connector sync event is audit-logged with full request metadata (timestamp, source IP, authenticated identity, response status) and stored immutably for 7 years to meet AI Act Article 12 logging obligations and GDPR record-keeping requirements under Article 30.
            """;

    // ─── Document 7: Data Handling & GDPR ─────────────────────────────
    private static final String REGU_DATA_HANDLING = """
            # Regu Data Handling and GDPR Compliance

            ## Data Residency

            All Regu customer data — compliance assessments, uploaded documents, embedding vectors, audit logs, and query histories — is stored exclusively in EU regions. The primary data centre is Frankfurt, Germany (AWS eu-central-1 equivalent on Hetzner Cloud), with a secondary replica in Dublin, Ireland. No customer data is stored, processed, or transmitted outside the EU/EEA under any circumstances. Regu's architecture was designed from day one for EU data residency, not retrofitted. This directly addresses the Schrems II ruling's requirement that EU data not be subject to US surveillance laws: Regu has no US-based sub-processors that handle personal data, and production data never leaves EU jurisdiction.

            ## Personal Data Minimization

            Regu's compliance assessment pipeline is designed to operate without requiring personal data. Assessments operate on system descriptions, technical specifications, and regulatory text — not on individual user records. When customers upload documents for context-aware assessment, an automatic PII detection pipeline runs before any text is embedded or stored. The pipeline uses a named entity recognition (NER) model fine-tuned on GDPR-sensitive entity types (names, addresses, national ID numbers, email addresses, phone numbers) combined with regex detectors for known structured formats (IBAN, EU VAT numbers, passport formats). Detected PII is redacted to a [REDACTED:TYPE] placeholder before the text reaches the chunking and embedding pipeline. The original unredacted text is never stored — only the redacted version enters the vector store.

            ## Encryption

            Data at rest is encrypted using AES-256-GCM for all PostgreSQL storage volumes. This applies to assessment data, document chunks, embedding vectors, and audit logs. Data in transit uses TLS 1.3 exclusively — no older TLS versions or plaintext HTTP are accepted on any Regu endpoint. Enterprise-tier customers can bring their own encryption keys (customer-managed keys, CMK) via integration with AWS KMS or Azure Key Vault. In the CMK configuration, Regu's application holds only an encrypted data key; the master key never leaves the customer's key management system. Database-level encryption is complemented by application-level field encryption for particularly sensitive fields: the full raw content of uploaded documents is encrypted at the application layer before being written to the database.

            ## Sub-processors and Data Processing Agreement

            Regu maintains a full published sub-processor list, updated within 30 days of any change. Current sub-processors involved in processing customer personal data are limited to: Hetzner Online GmbH (infrastructure hosting, EU-based), Anthropic PBC (LLM inference — only anonymized query text; covered by Anthropic's EU data processing terms), and Voyage AI Inc. (embedding generation — only text fragments, not full documents, and covered by Voyage AI's DPA). A GDPR Article 28-compliant Data Processing Addendum (DPA) is available as standard for all paying customers. Standard Contractual Clauses (SCCs) under Commission Decision 2021/914 are in place for the two US-based sub-processors, covering the specific processing activities. No cross-border data transfers occur for stored personal data — only transient API calls for inference and embedding.

            ## Right to Erasure (Article 17 GDPR)

            Customer-initiated full deletion of all associated personal data is fulfilled within 7 days of request. The deletion process covers: assessment records and associated queries, uploaded document raw content and redacted versions, and embedding vectors generated from customer documents. Embedding tombstoning ensures no vector representation survives deletion — the chunk record is deleted from PostgreSQL (cascading to the pgvector index), and the HNSW index is rebuilt to remove the deleted vectors from the ANN graph. Deletion requests are acknowledged within 24 hours and confirmed upon completion with a signed deletion certificate.

            ## Audit Logging

            Every data access event in Regu — API calls, document uploads, assessment queries, admin actions — is logged immutably in the audit_logs table. Logs are append-only at the database level: the application role has INSERT permission only on audit_logs, no UPDATE or DELETE. Log retention is 7 years, meeting both the AI Act Article 12 logging obligations for high-risk AI systems and GDPR Article 30 record-of-processing-activities requirements. Audit logs are queryable by the customer through the admin dashboard, filterable by event type, date range, and authenticated identity. Regu's own operations team access to customer audit logs is restricted to named individuals and requires dual approval via the access management system.
            """;

    /**
     * Returns all 7 seed documents as IngestDocumentRequest objects.
     */
    public List<IngestDocumentRequest> getSeedDocuments() {
        return List.of(
                new IngestDocumentRequest(
                        "regu_overview.md",
                        "README",
                        "/docs/regu_overview.md",
                        REGU_OVERVIEW
                ),
                new IngestDocumentRequest(
                        "regu_architecture.md",
                        "ARCHITECTURE",
                        "/docs/regu_architecture.md",
                        REGU_ARCHITECTURE
                ),
                new IngestDocumentRequest(
                        "regu_security.md",
                        "TECHNICAL_DOC",
                        "/docs/regu_security.md",
                        REGU_SECURITY
                ),
                new IngestDocumentRequest(
                        "regu_api_spec.md",
                        "API_SPEC",
                        "/docs/regu_api_spec.md",
                        REGU_API_SPEC
                ),
                new IngestDocumentRequest(
                        "regu_compliance_scope.md",
                        "LEGAL_CORPUS",
                        "/docs/regu_compliance_scope.md",
                        REGU_COMPLIANCE_SCOPE
                ),
                new IngestDocumentRequest(
                        "regu_integrations.md",
                        "TECHNICAL_DOC",
                        "/docs/regu_integrations.md",
                        REGU_INTEGRATIONS
                ),
                new IngestDocumentRequest(
                        "regu_data_handling.md",
                        "TECHNICAL_DOC",
                        "/docs/regu_data_handling.md",
                        REGU_DATA_HANDLING
                )
        );
    }
}
