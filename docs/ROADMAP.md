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
- **Phase G done**: dashboard gained a "Discovery lead time trend" line
  chart (`LeadTimeTrendChart`, x-axis = correlation `created_at`, y-axis
  = `lead_time_days`, mirrors `NewInvestmentsChart`/`ScanSuccessChart`), a
  "Coverage breakdown" panel (`CoverageBreakdown`: Tier A/B monitored
  counts + developer/discovery/aggregator municipality coverage, each as
  a mini progress bar), and a "Sources needing attention" panel
  (`SourcesNeedingAttention`: stale `source_snapshot` rows, `BLOCKED`
  developers, municipalities with any `BLOCKED` coverage category - each
  linking to its full page instead of duplicating it).

See `docs/ARCHITECTURE.md` "Implemented (phase 5/6, ...)" sections and
`docs/SOURCES.md` for the authoritative, detailed record.

## Next phases (in priority order)

### Phase D — Per-field provenance — DONE, see above.

### Phase E — Remaining Tier B `CANDIDATE` developers — DONE, see above.

Villa (Tier A) and Cavallia/BTM/Constructa Plus/Virke/SGI/FB Antczak
(Tier B) remain `BLOCKED`/`CANDIDATE` with no known working URL - do not
re-investigate unless new information surfaces.

### Phase F — Candidate status mutation + aggregator-only discovery view — DONE, see above.

### Phase G — Dashboard enhancements — DONE, see above.

All roadmap phases (D-G) from this file are now complete. Remaining
work is tracked in "Lower-priority / explicitly deferred" below and in
`docs/ARCHITECTURE.md` "Not yet implemented" - nothing currently
prioritized above those.

## Lower-priority / explicitly deferred (see AGENTS.md + docs/ARCHITECTURE.md)

- **Done**: 2 more discovery sources implemented - `srem-wz` (Gmina Śrem
  BIP, a two-step fetch since its register is split one page per calendar
  year - see `SremWzSource`/`SremWzParser` KDoc) and
  `murowana-goslina-obwieszczenia` (Gmina Murowana Goślina BIP). All other
  previously `BLOCKED`/`NOT_IMPLEMENTED` municipalities were re-verified
  live; several (Kleszczewo, Dopiewo, Skoki, Stęszew) turned out to have
  migrated off their old JS-SPA/dead platforms to real server-rendered
  sites, but no adapter could be built yet for them this session (see
  `registry.DiscoverySourceRegistry` for the specific reason per
  municipality - re-check Dopiewo/Skoki again in the future, they're the
  closest to being real).
  - **Found and fixed a real bug while doing this**: `LocationCatalog.findIn`
    used `\b` word boundaries, which Java defines using ASCII `[a-zA-Z0-9_]`
    only - so it silently failed to match the *majority* of this catalog's
    own entries (`Poznań`, `Śrem`, `Łowęcin`, ...) whenever adjacent to
    punctuation/whitespace, since their boundary characters are Polish
    diacritics. Fixed with explicit `\p{L}`/`\p{N}` lookarounds instead of
    `\b`. This affects every call site (aggregator-only-discovery
    location matching, developer-candidate municipality assignment, every
    discovery parser's `location` field) - a correctness win beyond just
    the two new sources.
- **Done**: Otodom explicitly skipped, per project decision to never add
  a headless-browser dependency (AGENTS.md/docs/ARCHITECTURE.md) - not
  re-investigated.
- **Done**: fuzzy developer-candidate deduplication -
  `domain.DeveloperNameMatcher` strips common Polish legal-entity suffixes
  (`Sp. z o.o.`, `S.A.`, `Sp. k.`, spelled-out forms, ...) before
  comparing, used by both `DeveloperRegistry.findByName` and
  `DeveloperCandidateRepository.findByName` so "ABC Development" and "ABC
  Development Sp. z o.o." are recognized as the same developer.
- **Done**: signal-to-signal correlation - `/signals` groups signals
  client-side by `source:reference` (multiple filing stages of the same
  case, e.g. "wszczęcie postępowania" -> "decyzja końcowa", share one
  case reference within a single source's own numbering scheme). Shows a
  "N stages" badge per row and, when expanded, a chronological case
  history list. No backend/scan-time work needed - this is a plain
  reference-equality group-by over data already fetched, not a
  fuzzy/feature-based match like investment<->signal correlation, so it
  stays purely presentational (`ExpandableTableRow` gained an optional
  `expandedExtra` slot for this).

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
