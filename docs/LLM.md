# Local LLM (Ollama) integration

## Role

The LLM is an **interpretation and ranking layer**, never a source of
truth. It never decides:

- whether an investment is new (that's `ChangeDetector`, deterministic)
- deduplication/identity (canonical key, deterministic)
- exact price/area extraction when a parser already has it (parsers only)
- source validation or persistence decisions

`DeterministicScorer` always computes the numeric comparison
(`InvestmentAnalysis.investmentScore`/`referenceProfileScore`) against a
`ReferenceInvestmentProfile`. The LLM only supplies
`InvestmentAnalysis.priority` and `.reason` - qualitative interpretation
on top of facts the deterministic pipeline already established - and only
when it returns a well-formed response.

## Enabling it

Enabled by default (`investment-monitor.llm.enabled: true` in
`application.yml`). `OllamaInvestmentAnalyzer` is the sole
`InvestmentAnalyzer` bean - when the LLM is enabled but Ollama isn't
installed or reachable, every call fails gracefully and the analyzer
falls back to a fully deterministic result (see "Failure handling"
below), so a fresh checkout still runs `./gradlew bootRun` successfully
with zero LLM setup. A startup log line reports whether Ollama was
actually reachable (`OllamaClient.probeAtStartup()`), so a missing local
install is visible immediately rather than silently degrading
investment-by-investment.

To get LLM-enhanced `priority`/`reason` text instead of the deterministic
fallback:

1. Install [Ollama](https://ollama.com) locally.
2. Pull a model, e.g.:
   ```bash
   ollama pull qwen2.5:7b
   ```
   For better multilingual/JSON-compliance results (at the cost of more
   RAM, ~12 GB), `qwen2.5:14b` is recommended if your machine can run it:
   ```bash
   ollama pull qwen2.5:14b
   ```
3. Set the model in `application.yml` if you pulled a different one than
   the default:
   ```yaml
   investment-monitor:
     llm:
       enabled: true
       base-url: http://localhost:11434
       model: qwen2.5:14b
       timeout-seconds: 60
   ```
4. Run `./gradlew bootRun` as usual.

## Disabling it

To skip the Ollama call attempt entirely and always use the deterministic
result (e.g. on a machine that will never run Ollama):

```yaml
investment-monitor:
  llm:
    enabled: false
```

## How a call works

1. `InvestmentPromptBuilder` builds a compact, structured-facts prompt -
   **never raw HTML**, and never a fact the deterministic pipeline hasn't
   already extracted (investment fields, location profile scores,
   reference profile configuration).
2. `OllamaClient.generate()` calls Ollama's `/api/generate` endpoint using
   the JDK's built-in `HttpClient` (no Spring MVC/WebFlux dependency -
   this stays a one-shot CLI tool, not an embedded web server; see
   ADR-001).
3. The response is parsed as JSON into `LlmInvestmentInterpretation`
   (`attractiveness`, `strongestPositives`, `risks`,
   `locationPromising`, `plotUnusuallyAttractive`, `worthManualReview`,
   `missingInformation`, `reason`).
4. Successful, well-formed responses are cached in the `llm_analysis`
   table, keyed by investment canonical key + prompt hash - unchanged
   inputs are a cache hit, not a repeated model call.

## Failure handling

Every failure mode degrades gracefully to a deterministic result, never
an exception:

| Failure | Behavior |
|---|---|
| LLM disabled (`enabled: false`) | No HTTP call is attempted at all; analyzer returns the deterministic result immediately |
| Ollama unreachable / network error | `OllamaClient.generate()` returns `null`; analyzer falls back to a deterministic priority from `DeterministicScorer`'s numeric score |
| Timeout | Same as above (caught by `runCatching`) |
| Malformed/non-JSON response | Jackson parse failure caught; same deterministic fallback |
| Well-formed but missing `attractiveness` | Falls back to the deterministic-score-derived priority for that field only |

This is unit-tested in `OllamaClientTest` (using a local JDK
`HttpServer`, no real network access) and `OllamaInvestmentAnalyzerTest`
(well-formed response, malformed response, unreachable server).

## Structured output only

The prompt explicitly demands a single JSON object matching a fixed
schema and nothing else. Arbitrary prose is never parsed - if the model
doesn't return valid JSON matching `LlmInvestmentInterpretation`, the
response is treated as a failure (see above), not partially salvaged.
