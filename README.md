<div align="center">

# GTM-Zero

**The AI Sales Engineer that handles technical objections in real time —
with citations to your own docs.**

[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Next.js 15](https://img.shields.io/badge/Next.js-15-000?logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![Postgres + pgvector](https://img.shields.io/badge/Postgres%20+%20pgvector-16-336791?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Claude Sonnet](https://img.shields.io/badge/Claude-Sonnet-8A6BCC)](https://www.anthropic.com/claude)
[![Voyage AI](https://img.shields.io/badge/Voyage--3--large-Embeddings-1F2937)](https://www.voyageai.com/)

</div>

---

GTM-Zero is a live retrieval-augmented sales engineer. Drop your product
documentation in, and every technical objection a prospect throws at you
gets answered in seconds — **streaming**, **cited**, and **verifiable**
against the chunks of source text the answer was built from.

Built as a START Munich pitch demo (May 2026), but the pipeline is real:
real RAG, real streaming SSE, real citation validation. No mocks on the
hot path.

```
┌──────────────────┐   POST /objections/stream   ┌──────────────────┐
│   Next.js 15     │ ──────────────────────────► │  Spring Boot 3   │
│   (App Router)   │ ◄── SSE: started, retrieved │  (Reactor / Flux)│
│  Framer Motion   │       token*, completed     │                  │
└────────┬─────────┘                             └────────┬─────────┘
         │                                                │
         │                                       ┌────────┴────────┐
         │                                       │  Voyage-3-large │
         │                                       │  1024-dim vec   │
         │                                       └────────┬────────┘
         │                                                │
         │                                       ┌────────┴────────┐
         │                                       │ Postgres 16 +   │
         │                                       │ pgvector (HNSW) │
         │                                       └────────┬────────┘
         │                                                │
         │                                       ┌────────┴────────┐
         │                                       │  Claude Sonnet  │
         │                                       │  streaming      │
         │                                       └─────────────────┘
```

## Why it exists

Most "AI for sales" demos generate plausible text. That fails the moment a
serious technical buyer asks *"on what basis?"* — and serious buyers always
ask. GTM-Zero answers with the chunk ID, document title, and snippet next
to every claim. **If a sentence doesn't cite a real chunk, the validator
flags it before the answer ships.**

That's the difference between a chat playground and something a founder
can put on a sales call.

## What's in the box

| Surface | What it does |
| --- | --- |
| **Dashboard** | Live pulse of recent objections, outreach, ingestions, with citation chips inline. |
| **Objection Handling** | Split-screen "live call" scene. Press 1/2/3 → token-streamed answer with cited markers `[1] [2] [3]` resolving into source chips. |
| **Outreach** | Hyper-personalized first-touch messages from prospect signals (LinkedIn URL, GitHub, tech stack hints). |
| **Documents** | Upload pipeline → chunking → Voyage embeddings → pgvector index. Idempotent, content-hashed. |

## The RAG pipeline, in one diagram

```
ingest        →  chunk         →  embed         →  store
(MD/HTML/PDF)    (sentence       (Voyage-3-large    (pgvector HNSW
                  windows)        1024-dim)          cosine sim)

retrieve      →  rerank        →  prompt        →  stream         →  validate
(top-K vector    (BM25 hybrid    (Claude Sonnet    (Reactor Flux     (citation
 search)          rescore)        with markers)    SSE)              coverage ≥ 0.82)
```

Every objection answer hits **all** of those steps. The Citation Validator
is the gate: if marker coverage drops below 0.82, the answer is rejected
upstream of the user.

## Quick start

### Prerequisites

- **Java 21** (try `sdkman: sdk install java 21-tem`)
- **Node 20+**
- **Docker Desktop**
- API keys: [Anthropic](https://console.anthropic.com), [Voyage AI](https://dash.voyageai.com)

### 1. Configure

```bash
cp .env.example .env
# fill in ANTHROPIC_API_KEY and VOYAGE_API_KEY
```

### 2. Database

```bash
cd backend
docker compose up -d
```

Postgres 16 + pgvector starts on `localhost:5432`. Schema migrates
automatically on first backend boot.

### 3. Backend

```bash
cd backend
export $(cat ../.env | xargs)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Wait for `Started GtmZeroApplication`. Health check:

```bash
curl http://localhost:8080/api/v1/health
# {"status":"UP","service":"gtm-zero","db":"UP",...}
```

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:3000>. The top-right pill flips to
**API: Connected ✓** when the backend handshake succeeds.

## API surface

| Method | Path | What |
| --- | --- | --- |
| `GET`  | `/api/v1/health` | DB + service liveness |
| `GET`  | `/api/v1/dashboard` | Metrics + activity feed |
| `POST` | `/api/v1/objections/stream` | **SSE.** `started → retrieved → token* → completed` |
| `POST` | `/api/v1/objections` | Non-streaming variant — same payload, blocks until done |
| `GET`  | `/api/v1/objections/recent?limit=N` | Recent answered objections (dashboard feed) |
| `GET`  | `/api/v1/objections/session/{sessionId}` | Per-session history |
| `POST` | `/api/v1/outreach/generate` | Generate first-touch message |
| `POST` | `/api/v1/outreach/{id}/send-mock` | Mark message as sent |
| `GET`  | `/api/v1/prospects?linkedinUrl=…` | Prospect lookup |
| `POST` | `/api/v1/documents/ingest` | Upload + chunk + embed |

## Repo layout

```
gtm-zero/
├── backend/                # Spring Boot 3 / Java 21
│   ├── src/main/java/com/gtmzero/
│   │   ├── controller/     # REST + SSE entrypoints
│   │   ├── service/        # RAG pipeline, embeddings, validation
│   │   ├── entity/         # JPA entities (Document, Chunk, Prospect…)
│   │   ├── repository/     # Spring Data JPA + native pgvector queries
│   │   └── dto/            # Wire types — records all the way down
│   └── docker-compose.yml  # Postgres + pgvector
├── frontend/               # Next.js 15 (App Router) / React 19
│   ├── app/(app)/          # Dashboard, Objection, Outreach, Documents
│   ├── components/         # ui/, primitives/, shell/, dashboard/, objection/
│   └── lib/                # streaming SSE client, types, motion tokens
└── docs/                   # Pitch runbook, Q&A bank, rehearsal protocol
```

## Design dialect

The frontend has one design language and sticks to it:

- **No emoji, no gradient text, no glow borders, no parallax, no typewriter cursors.**
- Geist (sans) for UI, Geist Mono for metrics, Instrument Serif for hero italics only.
- Single accent color (amber). Single motion easing (`easeOutQuint`).
- `animate-ping` exists in exactly one place: the recording-status dot on the live-call scene. Justified because that scene *is* a live session.

## Tests

```bash
cd backend
./mvnw test
```

57+ tests covering: chunking, embedding flow, citation validation, RAG end-to-end, ingestion idempotency, outreach generation, controller contracts.

## Pitch demo

The `/objection` route is the 60–90 second core of the pitch. Press **1**,
**2**, or **3** with no mouse — three seeded technical objections fire
through the live RAG pipeline against a seeded "Marie Dubois @ Lumeon
Health" prospect. Each completes in ~7s with ≥ 0.82 citation coverage on
seeded technical docs.

See [`docs/pitch-day-runbook.md`](docs/pitch-day-runbook.md) for the
exact run-of-show.

## License

Proprietary — START Munich demo build. Contact the maintainers before
reusing.
