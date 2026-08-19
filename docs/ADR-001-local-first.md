# ADR-001: Local-first

## Status

Accepted

## Context

The tool's job is to catch new housing investments before they appear on
aggregator portals (Otodom and similar). That means it needs to run
unattended, repeatedly, over a long period of time - and it operates on
data that only matters to a single person (the investment preferences and
scan history are not shareable, multi-tenant data).

Two broad architectures were considered:

1. **Cloud-hosted**: a scheduled job (or small server) running on rented
   infrastructure, with a managed database and possibly a hosted LLM API.
2. **Local-first**: scraping, persistence, and any LLM inference run
   entirely on a machine the user controls - no server to operate, no
   external dependency to keep paying for or trusting.

## Decision

Run scraping, persistence, and inference locally:

- **Persistence**: SQLite, a single file on disk, no database server.
- **Scraping**: plain HTTP via Jsoup, triggered by `./gradlew bootRun` -
  no scheduler service, no queue, no worker fleet.
- **Inference** (future): a local LLM (Ollama + Qwen), not a hosted API.

The tool remains a single Kotlin/Spring Boot process plus a local Next.js
dashboard, both pointed at the same SQLite file.

## Consequences

**Gained:**
- No hosting cost, no server to patch or monitor.
- No data leaves the machine - relevant since the tool tracks personal
  buying preferences and a history of what has been seen.
- Trivial backup story: copy one `.db` file.
- Fast local development loop - no deploy step to test a change.

**Traded away:**
- No built-in scheduling; the user is responsible for a cron job (or
  running it manually) to get repeated scans.
- No multi-user support - the tool is explicitly single-tenant.
- SQLite's single-writer model means the Kotlin pipeline and the Next.js
  frontend must coordinate around concurrent writes (see
  `busy_timeout` in `application.yml` and `lib/db.ts`, and the
  in-memory scan-lock in `frontend/app/api/scan/route.ts`).
- A local LLM is slower and less capable than a frontier hosted model;
  acceptable here because the LLM is an interpretation layer, not a
  source of truth (see ADR-002).
