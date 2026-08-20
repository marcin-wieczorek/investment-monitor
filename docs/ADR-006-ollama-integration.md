# ADR-006: Local Ollama via JDK `HttpClient`, enabled by default with deterministic fallback

## Status

Accepted (amended - see "Amendment" below)

## Context

The project needed a real local-LLM integration behind
`InvestmentAnalyzer`, replacing the no-op placeholder,
while preserving two hard constraints from ADR-001 (local-first) and
ADR-002 (deterministic core): no cloud dependency, and the LLM must never
be able to break a scan.

Two implementation questions came up:

1. **HTTP client**: Spring's `RestClient`/`WebClient` are the idiomatic
   Spring Boot choice, but both require `spring-boot-starter-web`
   (embedded Tomcat + full MVC stack) as a transitive dependency - a lot
   of weight for a single outbound POST request in a one-shot CLI tool
   that isn't a web server.
2. **Availability**: not every environment running this tool will have
   Ollama installed. The tool must work identically with or without it.

## Decision

- `OllamaClient` (`llm/OllamaClient.kt`) uses the JDK's built-in
  `java.net.http.HttpClient` (available since Java 11; this project
  targets Java 21) instead of adding `spring-boot-starter-web`. No new
  runtime dependency, no embedded server.
- Every `OllamaClient` method is wrapped in `runCatching` and returns
  `null`/`false` on any failure (network error, timeout, non-2xx status,
  malformed JSON) - it never throws out of the client.
- `OllamaInvestmentAnalyzer` is the sole `InvestmentAnalyzer` bean.
  `MonitoringService`'s constructor is unchanged (still takes a single
  `InvestmentAnalyzer`).

## Amendment (LLM enabled by default)

The original decision registered two mutually exclusive
`InvestmentAnalyzer` beans via `@ConditionalOnProperty`:
`OllamaInvestmentAnalyzer` when `investment-monitor.llm.enabled=true`,
and a separate `DefaultInvestmentAnalyzer` (deterministic-only) for the
default `false` case. In practice this meant two classes independently
reimplementing the same deterministic scoring/priority/reason logic,
with only a passing code comment ("uses the exact same deterministic
score via `DeterministicAnalysisSupport`") keeping them in sync.

This was collapsed into a single class: `OllamaInvestmentAnalyzer` now
checks `investment-monitor.llm.enabled` internally. When `false`, it
returns the deterministic result immediately, without attempting an
Ollama call - this is the *same* code path used as the fallback when the
LLM is enabled but unavailable, so "LLM off" and "LLM failed" can never
silently drift apart into two different deterministic behaviors.

`investment-monitor.llm.enabled` now defaults to `true` in
`application.yml`. This does not weaken the local-first guarantee: when
Ollama isn't installed or reachable, `OllamaClient.generate()` still
fails gracefully (5s connect timeout, `runCatching`) and the analyzer
still falls back to the identical deterministic result a disabled LLM
would produce - a fresh checkout still runs `./gradlew bootRun`
successfully with zero LLM setup, it just also logs one startup line
(`OllamaClient.probeAtStartup()`, a `@PostConstruct` best-effort
`isAvailable()` check) reporting whether Ollama was actually reachable,
so the difference between "LLM not installed" and "LLM installed and
working" is visible immediately instead of only inferable from a
per-investment reason string.

## Consequences

**Gained:**
- No new heavyweight dependency for a single HTTP call.
- `./gradlew test` never touches a real network (verified with a local
  JDK `HttpServer` fixture in `OllamaClientTest`/`OllamaInvestmentAnalyzerTest`,
  not a real Ollama instance).
- A misconfigured, absent, or explicitly disabled local LLM degrades to
  fully deterministic behavior (see ADR-005) rather than failing the scan
  or hanging indefinitely (5s connect timeout, configurable read timeout).
- Exactly one code path computes the deterministic fallback, eliminating
  the risk of the two-analyzer split drifting apart.
- A user who installs Ollama gets LLM-enhanced interpretation with zero
  configuration beyond installing it; a user who never installs it gets
  identical deterministic behavior to before, plus one informational
  startup log line.

**Traded away:**
- No streaming support, retries, or connection pooling beyond what the
  JDK client provides by default - acceptable for a one-shot tool making
  at most a few dozen LLM calls per scan (one per newly-detected
  investment), not a high-throughput service.
- If the project later needs a full HTTP client feature set (circuit
  breakers, structured client-side metrics, ...), revisiting this
  decision in favor of a dedicated HTTP client dependency (without
  necessarily pulling in all of `spring-boot-starter-web`) would be
  reasonable.
