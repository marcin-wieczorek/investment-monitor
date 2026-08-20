# Roadmap — next phases

Context file for continuing the evolution described in `AGENTS.md` (the
original full-scope prompt). Read `AGENTS.md` first for the complete
vision; this file tracks **what's already done** vs **what's next**,
concretely, so a future session can pick up without re-deriving the plan.

Last updated: after the session that added developer/municipality
registries, 16 developer + 4 discovery adapters, deterministic scoring
pipeline, discovery lead time, and price/watchlist UI (commit
`b35fa1f`).

## Already implemented (do not redo)

- Domain foundation: `Developer`, `DeveloperCandidate`, `Municipality` +
  registries (`registry/DeveloperRegistry.kt`, `registry/MunicipalityRegistry.kt`,
  `registry/DiscoverySourceRegistry.kt`), mirrored into SQLite via
  `V5__developer_municipality_registry.sql`.
- 18 developer adapters (Chronos, Greenbud + 16 new: ATAL, Agrobex,
  Spravia, Duda, Develia, Jakon Inwest, ROBYG, Linea, Murapol, Ataner,
  Konimpex, Pekabex, EBF, GGW, JakśBud, UWI), 5 discovery adapters
  (Swarzędz + Czerwonak, Tarnowo Podgórne, Suchy Las, Poznań ULICP), 1
  aggregator (RynekPierwotny).
- Deterministic scoring always runs (`DefaultInvestmentAnalyzer` +
  `DeterministicAnalysisSupport`), persisted via `InvestmentScoreRepository`
  / `investment_score` table (`V6__investment_score_watchlist.sql`).
- Discovery lead time: `CorrelationRepository.findAllWithLeadTime()`,
  rendered in the console report and on `/correlations`/dashboard.
- Watchlist: `investment_state.watched` column, `PUT /api/investments/[id]/watch`.
- Frontend: `/developers`, `/coverage` pages; investments list with
  source-category badges, advanced filter panel (range sliders for
  area/price), score badge column, price column; investment detail with
  scoring breakdown, price/ratio badges, watch button.
- **Phase D done**: per-field provenance. `MonitoringService.recordInvestmentEvidence`/
  `recordSignalEvidence` now write one `SourceEvidence` row per actual
  non-null fact (`name`/`location`/`propertyType`/`units`/`houseArea`/
  `plotArea`/`price`/`status`/`imageUrl` for investments;
  `title`/`signalType`/`detectedAt`/`location`/`reference` for signals),
  not one `"investment"`/`"signal"` placeholder row. Frontend
  `investment-detail-view.tsx` groups evidence by `field_name` and shows
  a "confirmed by N sources" badge when 2+ distinct `source_id`s agree on
  the same fact.
- **Phase E done**: the 7 remaining Tier B `CANDIDATE` developers with a
  verified URL now have adapters and are `MONITORED`: `cordia`, `ronson`,
  `sivanet`, `mj`, `area`, `inwestycje_wielkopolski`, `vastbouw` (25
  developer adapters total). `V7__promote_tier_b_candidates.sql` updates
  their `developer_registry` rows in place (status, adapter_source_id,
  corrected `investment_list_urls`). See `docs/SOURCES.md` "Implemented
  developer sources" for per-developer notes (several currently list
  zero or non-Poznań investments - documented, not worked around).
- **Phase F done**: `PUT /api/developers/candidates/[id]/status` (body
  `{ status }`, one of `ACCEPTED`/`REJECTED`/`IMPLEMENTED`/`BLOCKED`)
  wraps `DeveloperCandidateRepository.updateStatus`; `/developers` has
  Accept/Reject/Block buttons per `NEW`/`REVIEW_REQUIRED` candidate row.
  Aggregator-only discoveries are now persisted, not just reported once
  per scan: `investment.aggregator_only_discovery` (`V8__aggregator_only_discovery_flag.sql`)
  is recomputed by `MonitoringService.updateAggregatorOnlyDiscoveryFlags`
  for every current aggregator investment on every scan (reusing
  `LocationCatalog`, the same deterministic matching
  `findAggregatorOnlyDiscoveries` already used for the console report -
  kept single-sourced in Kotlin rather than re-derived in SQL/JS). The
  dashboard has an "Aggregator-only discoveries" stat card linking to
  `/investments?aggregatorOnly=1`, and `/investments` has a matching
  toggle + row badge.

See `docs/ARCHITECTURE.md` "Implemented (phase 5/6, ...)" sections and
`docs/SOURCES.md` for the authoritative, detailed record.

## Next phases (in priority order)

### Phase D — Per-field provenance — DONE, see above.

### Phase E — Remaining Tier B `CANDIDATE` developers — DONE, see above.

Villa (Tier A) and Cavallia/BTM/Constructa Plus/Virke/SGI/FB Antczak
(Tier B) remain `BLOCKED`/`CANDIDATE` with no known working URL - do not
re-investigate unless new information surfaces.

### Phase F — Candidate status mutation + aggregator-only discovery view — DONE, see above.

### Phase G — Dashboard enhancements

1. "Discovery lead time trend" chart (`ApexCharts` line/area, x-axis =
   correlation date, y-axis = lead time days) - mirrors
   `new-investments-chart.tsx`/`scan-success-chart.tsx` pattern.
2. "Coverage progress" mini-chart or stat breakdown (Tier A vs Tier B
   monitored count, municipality coverage by category) beyond the single
   stat cards that exist today.
3. "Sources needing attention" section - sources with `BLOCKED` status
   (from `DiscoverySourceRegistry`/`DeveloperRegistry`) or stale
   `source_snapshot` rows, surfaced together rather than requiring a
   visit to `/sources` + `/coverage` + `/developers` separately.

## Lower-priority / explicitly deferred (see AGENTS.md + docs/ARCHITECTURE.md)

- Additional discovery sources beyond the 5 implemented municipalities -
  see `registry/DiscoverySourceRegistry.kt` for the full blocked/
  not-implemented list and documented reasons per municipality.
- Otodom aggregator (requires headless browser, deliberately out of
  scope).
- Fuzzy developer-candidate deduplication (currently exact-name match
  only via `DeveloperRegistry.findByName`/`DeveloperCandidateRepository.findByName`).
- Signal-to-signal correlation (grouping filing stages of the same
  zoning case by shared `reference`).

## Working conventions to preserve

- Never invent a URL/selector - verify against real HTML first (`curl`
  the actual page, inspect it, only then write a parser). Document
  anything unimplementable as `BLOCKED`/`PLANNED` with a real reason,
  never silently skip it or fake an adapter.
- Every new parser needs a captured fixture + a test asserting every
  field it actually publishes, leaving unpublished fields `null`.
- Flyway migrations are append-only - never edit `V1`-`V6`, add `V7+`.
- Run `./gradlew test` (backend) and `npm run build` (frontend
  typecheck) before considering a phase done; a live `./gradlew bootRun`
  smoke-test is worth doing when touching `MonitoringService`/adapters
  end-to-end, then delete the generated `investment-monitor.db*` files
  (gitignored, but avoid leaving them lying around locally) before
  committing.
