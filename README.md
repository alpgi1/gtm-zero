# GTM-Zero

AI Sales Engineer for Technical Founders — auto-generates discovery calls, proposals, and demo scripts from your product docs.

**Status:** Under construction (Part 1 / 10)

---

## Prerequisites

- Java 21
- Maven (via `./mvnw`)
- Node.js 20+
- Docker Desktop

## Running locally

### 1. Database

```bash
cd backend
docker compose up -d
```

### 2. Backend

```bash
cd backend
cp ../.env.example ../.env   # fill in API keys
export $(cat ../.env | xargs)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) — the page shows **API: Connected ✓** when the backend is up.

### Health check

```bash
curl http://localhost:8080/api/v1/health
```
