# Discovery sources

## Why discovery sources exist

The primary business goal is early detection - ideally before a developer
publishes a normal sales page. Municipal administrative processes (zoning
conditions, building permits, environmental decisions) are public record
and frequently precede a marketable investment by weeks or months. A
`DiscoverySource` captures this evidence.

## `InvestmentSignal` vs `Investment`

A discovery source returns `InvestmentSignal`, never `Investment`. A
signal is evidence that *something* is being planned or permitted - not
proof that a specific marketable investment exists. Deterministic
identity, deduplication and persistence apply equally to signals (same
`source:normalized-url` canonical-key scheme as `Investment`, see
ADR-002), but a signal never gets "enriched" into an `Investment`
automatically. `InvestmentCorrelator` links the two deterministically when
warranted (see `docs/ARCHITECTURE.md`); nothing invents an investment
name/price/unit-count from a signal.

### Signal types (`domain/InvestmentSignal.kt`)

`BUILDING_PERMIT`, `ZONING_DECISION`, `WZ_DECISION`, `MPZP_CHANGE`,
`PLANNING_APPLICATION`, `LAND_DEVELOPMENT_SIGNAL`, `ENVIRONMENTAL_DECISION`,
`MUNICIPAL_INVESTMENT_SIGNAL`, `DEVELOPER_LAND_ACQUISITION_SIGNAL`, `OTHER`.

## Implemented: Gmina Swarzędz zoning-conditions register

`SwarzedzWzSource` + `SwarzedzWzParser`
(`https://bip.swarzedz.pl/index.php?id=344`) parse the real, live
"Warunki zabudowy" (zoning conditions) register - a TYPO3-based,
server-rendered page with ~280 real decision documents at time of
writing, including genuine large-scale residential cases, e.g.:

> WAU.6730.23.2026 - budowa 74 budynków mieszkalnych jednorodzinnych w
> zabudowie bliźniaczej oraz 150 budynków mieszkalnych jednorodzinnych w
> zabudowie szeregowej ... Kruszewnia - decyzja końcowa

(A 224-house development across two typologies, in a village already in
the target geographic scope.)

**Parsing approach**: each decision is an `<a class="download">` link
whose text starts with a case reference (`WAU.6730.23.2026`) followed by a
free-text description. The publish date is read from the document's own
URL path (`.../2026/03_06_2026/23z2026_decyzja.pdf`), not from free text -
deterministic and independent of locale/formatting. Location is extracted
by matching known village names (`domain/LocationCatalog.kt`) inside the
description text. Documents without a parseable case reference ("Załącznik
graficzny" attachments) are skipped rather than guessed at.

**Known data-quality quirk**: the register occasionally reuses the exact
same document URL for two unrelated cases (a publishing mistake on the
municipality's side). Since identity follows the URL, this means one of
the two colliding signals doesn't survive persistence - documented as an
accepted, rare trade-off in the parser's KDoc rather than a reason to
diverge from the project's established canonical-key identity model.

## Implemented: Gmina Śrem zoning-conditions register (year-paginated)

`SremWzSource` + `SremWzParser` (`http://bip.srem.pl/public/?id=73563`)
are the only discovery source implemented so far whose register is split
one page per calendar year rather than a single evergreen feed. A single
hardcoded `LIST_URL` would silently start returning nothing every January,
so `SremWzSource.fetch()` does a two-step fetch instead: parse the index
page to find the current (highest) year's URL, then fetch and parse that
page for the actual announcements. Each announcement is a DOCX download
link (`a.nazwa_pliku`) with its creation date in a sibling `.wytworzyl_data`
metadata field.

## Implemented: Gmina Murowana Goślina obwieszczenia register

`MurowanaGoslinaObwieszczeniaSource` + `MurowanaGoslinaObwieszczeniaParser`
(`https://bip.murowana-goslina.pl/wiadomosci/9179/lista/1/obwieszczenia_inne`)
mixes zoning-conditions ("warunki zabudowy") and public-purpose siting
("lokalizacja inwestycji celu publicznego") decisions on the same feed,
classified per-item by keyword (see `toSignalType`). No per-item
publish date is available on the list page, so `detectedAt` falls back to
`Instant.EPOCH` - the same documented fallback `PoznanUlicpParser` already
uses when a date can't be parsed.

## Location extraction

Discovery parsers extract location by matching known place names
(`domain/LocationCatalog.kt`) as whole words in free text. This is
deliberately conservative: a signal whose text doesn't mention a
recognized location gets `location = null` rather than a guess, and is
therefore never correlated to an investment (see
`InvestmentCorrelator` - correlation requires a recognized location on
both sides).

## Adding a new municipality

1. Identify the municipality's real BIP (Biuletyn Informacji Publicznej)
   URL and inspect its actual HTML (`curl`/browser devtools) - do not
   guess selectors.
2. Confirm the content is reachable: either server-rendered directly (no
   JS execution required), or - if it's a client-side-rendered shell -
   reachable via the opt-in `PlaywrightPageFetcher` (see ADR-007; several
   implemented sources, e.g. Buk, Szamotuły, Pobiedziska, Kórnik, Dopiewo,
   needed this). Either way, confirm it's accessible without aggressive
   rate-limiting/blocking.
3. Capture a fixture (`./gradlew captureFixtures`, or manually if the
   target isn't wired into the CLI yet) and inspect it.
4. Implement `DiscoverySource` + a dedicated parser, anchored on stable
   structural markers (see `SwarzedzWzParser` for the pattern).
5. Add fixture tests covering the fields discovery signals need
   (municipality, location, signal type, title, reference, detected date,
   URL).
6. Add the source to `SourceVerificationCli`/`FixtureCaptureCli`.
7. If the municipality cannot be verified (client-side rendering,
   blocking, no accessible content), document it in `docs/SOURCES.md`
   under "Investigated but not implemented" - do not ship a fake adapter.

## Investigated municipalities

See `docs/SOURCES.md` "Investigated but not implemented" for the full,
maintained list of municipalities inspected but not currently
implementable (WAF/anti-bot blocking, DNS/transport failures, PDF-only
registers, or genuinely no usable content) - e.g. Kleszczewo and
Komorniki, inspected with real HTTP requests during this project and
found blocked by either an undocumented API or active anti-bot blocking.
