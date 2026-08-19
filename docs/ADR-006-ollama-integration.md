# ADR-006: Local Ollama via JDK `HttpClient`, disabled by default

## Status

Accepted

## Context

The project needed a real local-LLM integration behind
`InvestmentAnalyzer`, replacing the `NoOpInvestmentAnalyzer` placeholder,
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
- `OllamaInvestmentAnalyzer` is registered as a Spring bean only when
  `investment-monitor.llm.enabled=true`
  (`@ConditionalOnProperty`), with `NoOpInvestmentAnalyzer` registered
  for the (default) opposite case via the same mechanism. Exactly one
  `InvestmentAnalyzer` bean exists at a time - `MonitoringService`'s
  constructor is unchanged (still takes a single `InvestmentAnalyzer`).
- Configuration defaults to `enabled: false` in `application.yml` - a
  fresh checkout of this project runs `./gradlew bootRun` successfully
  with zero LLM setup.

## Consequences

**Gained:**
- No new heavyweight dependency for a single HTTP call.
- `./gradlew test` never touches a real network (verified with a local
  JDK `HttpServer` fixture in `OllamaClientTest`/`OllamaInvestmentAnalyzerTest`,
  not a real Ollama instance).
- A misconfigured or absent local LLM degrades to fully deterministic
  behavior (see ADR-005) rather than failing the scan or hanging
  indefinitely (5s connect timeout, configurable read timeout).

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
