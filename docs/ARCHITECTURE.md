# Architecture

CLI -> sources -> fetcher -> parser -> validation -> deterministic diff -> optional local LLM -> report -> trusted snapshot.

## Implemented (phase 1)

- `chronos` source: `ChronosSource` (Jsoup fetch) + `ChronosParser` (list page only).
- `SourceValidator`: fail-closed drop-threshold check.
- `ChangeDetector`: canonical-key diff against the last trusted snapshot.
- Persistence: JdbcTemplate repositories over SQLite (`investment`, `source_snapshot`, `monitoring_run`).
- `MonitoringService` + `ScanRunner`: one-shot orchestration triggered by `./gradlew bootRun`.
- `ScanReportRenderer`: plain-text scan report.

## Implemented (phase 2)

- `greenbud` source: second real developer (`GreenbudSource` + `GreenbudParser`), including
  house/plot area parsed directly from the list page (`PolishAreaFormat`).
- Generic detail-page scraping: `InvestmentDetailParser` (Strategy per investment URL/host) +
  `InvestmentDetailEnricher` (matches an investment to a parser by domain, best-effort - a
  missing/failing detail parser never fails a scan). Each investment can live on its own
  independent domain (e.g. Chronos investments each get a dedicated site), so parsers are
  matched by host, not by owning developer.
- `TercjaDetailParser`: first concrete detail parser (tercja.eu), enriches unit count,
  house area and plot area from the investment's own descriptive page.
- Enrichment + analysis only run for newly detected investments (one-time cost per
  investment, not per scan).
- `InvestmentAnalyzer` (LLM interpretation layer) + `NoOpInvestmentAnalyzer` placeholder:
  explicitly reports "not analyzed" rather than fabricating a score. No local LLM
  (Ollama/Qwen) is wired in yet.

## Not yet implemented (phase 3)

- Wiring a real `InvestmentAnalyzer` to a local LLM (Ollama + Qwen).
- Reference-profile scoring and location-profile data.
- Additional detail parsers for other Chronos/Greenbud investment sites.
- Raw HTML archival.
