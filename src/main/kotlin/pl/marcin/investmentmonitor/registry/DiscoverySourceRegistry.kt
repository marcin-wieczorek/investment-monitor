package pl.marcin.investmentmonitor.registry

/** Status of a municipal discovery source investigated for AGENTS.md section 11/12. */
enum class DiscoverySourceStatus { IMPLEMENTED, NOT_IMPLEMENTED, BLOCKED }

/**
 * One municipality's official discovery-source investigation outcome:
 * the BIP URL looked at, whether it was implementable, and why not when
 * it wasn't (see AGENTS.md "no fake implementations").
 */
data class DiscoverySourceEntry(
    val municipalityId: String,
    val bipUrl: String?,
    val status: DiscoverySourceStatus,
    val blockedReason: String?,
    val adapterSourceId: String?,
    /**
     * True when [blockedReason] identifies a JS SPA/client-side-rendered
     * site (see ADR-007) - i.e. a headless-browser fetcher could plausibly
     * unblock it, as opposed to a WAF/DNS/content-gap reason that a
     * browser wouldn't fix. Purely descriptive metadata: does not by
     * itself imply an adapter exists or that [status] is anything but
     * [DiscoverySourceStatus.BLOCKED]/[DiscoverySourceStatus.NOT_IMPLEMENTED].
     */
    val requiresBrowser: Boolean = false
)

/**
 * Explicit registry of municipal discovery-source investigation results
 * across the full target area (AGENTS.md section 11).
 *
 * This is intentionally more detailed than [MunicipalityRegistry]'s
 * `discoveryCoverage` field - it records *why* a municipality is blocked,
 * which is essential for anyone continuing this work later.
 */
object DiscoverySourceRegistry {

    val ALL: List<DiscoverySourceEntry> = listOf(
        DiscoverySourceEntry("swarzedz", "https://bip.swarzedz.pl/index.php?id=344", DiscoverySourceStatus.IMPLEMENTED, null, "swarzedz-wz"),
        DiscoverySourceEntry("czerwonak", "https://bip.czerwonak.pl/6469", DiscoverySourceStatus.IMPLEMENTED, null, "czerwonak-obwieszczenia"),
        DiscoverySourceEntry("tarnowo_podgorne", "http://bip2.tarnowo-podgorne.pl/6037", DiscoverySourceStatus.IMPLEMENTED, null, "tarnowo-podgorne-wz"),
        DiscoverySourceEntry("suchy_las", "https://bip.suchylas.pl/artykuly/planowanie-i-zagospodarowanie-przestrzenne-obwieszczenia-npp", DiscoverySourceStatus.IMPLEMENTED, null, "suchy-las-npp"),
        DiscoverySourceEntry("poznan", "https://bip.poznan.pl/bip/news/obwieszczenia-dotyczace-postepowan-o-ustalenie-lokalizacji-inwestycji-celu-publicznego-19,c,8440/", DiscoverySourceStatus.IMPLEMENTED, null, "poznan-ulicp"),
        DiscoverySourceEntry("srem", "http://bip.srem.pl/public/?id=73563", DiscoverySourceStatus.IMPLEMENTED, null, "srem-wz"),
        DiscoverySourceEntry("murowana_goslina", "https://bip.murowana-goslina.pl/wiadomosci/dzial/3454", DiscoverySourceStatus.IMPLEMENTED, null, "murowana-goslina-obwieszczenia"),
        DiscoverySourceEntry("kornik", "https://bip.kornik.pl/obwieszczenia-i-ogloszenia", DiscoverySourceStatus.IMPLEMENTED, null, "kornik-obwieszczenia", requiresBrowser = true),
        DiscoverySourceEntry("mosina", "http://bip.um.mosina.pl", DiscoverySourceStatus.NOT_IMPLEMENTED, "Re-verified: bip.mosina.pl is now just a directory of subsite tiles; the real BIP at bip.um.mosina.pl is server-rendered but its 'Planowanie przestrzenne' section only has MPZP/studium static pages, no obwieszczenia/case register found.", null),
        DiscoverySourceEntry("puszczykowo", "http://bip3.wokiss.pl/puszczykowo/bip/gospodarka-przestrzenna/obwieszczenia/postepowania-administracyjne.html", DiscoverySourceStatus.NOT_IMPLEMENTED, "Re-verified: migrated to a WOKISS-hosted BIP with a real, server-rendered 'Postępowania administracyjne' register, but every entry found so far is public-purpose infrastructure (water/sewer network), not residential warunki zabudowy; also publishes no per-item date.", null),
        DiscoverySourceEntry("kleszczewo", "https://bip.kleszczewo.pl", DiscoverySourceStatus.NOT_IMPLEMENTED, "Re-verified: migrated off Nefeni to a server-rendered Next.js BIP (bip-api.kleszczewo.pl). Its 'Obwieszczenia i ogłoszenia' category exists but only surfaced non-residential notices (road expropriation, cultural heritage, military) in this session - no dedicated warunki-zabudowy category found yet.", null),
        DiscoverySourceEntry("dopiewo", "https://bip.dopiewo.pl/kategorie/125-decyzje-o-warunkach-zabudowy", DiscoverySourceStatus.IMPLEMENTED, null, "dopiewo-wz", requiresBrowser = true),
        DiscoverySourceEntry("steszew", "http://bip.steszew.pl/index.php?id=14", DiscoverySourceStatus.NOT_IMPLEMENTED, "Re-verified: the BIP itself is reachable again (no longer a transport error), and has a real 'Zagospodarowanie Przestrzenne' section, but it only links to MPZP/studium/environmental pages - no obwieszczenia or case register found.", null),
        DiscoverySourceEntry("skoki", "https://skoki.nowoczesnagmina.pl/kategorie/116-decyzje-o-ustaleniu-lokalizacji-celu-publicznego-oraz-decyzje-o-warunkach-zabudowy-poprzedzone-decyzja-o-srodowiskowych-uwarunkowaniach", DiscoverySourceStatus.NOT_IMPLEMENTED, "Re-verified: migrated off Nefeni to the same new Next.js BIP platform as Kleszczewo/Dopiewo, and has a real, correctly-named combined celu-publiczne/warunki-zabudowy register - but it currently has only a single (non-residential, road/water infrastructure) entry, too little to build and verify a parser against.", null),
        DiscoverySourceEntry("buk", "https://bip.buk.gmina.pl", DiscoverySourceStatus.IMPLEMENTED, null, "buk-obwieszczenia", requiresBrowser = true),
        DiscoverySourceEntry("oborniki", "https://bip.umoborniki.nv.pl", DiscoverySourceStatus.NOT_IMPLEMENTED, "Fetchable via PlaywrightPageFetcher (ADR-007) - homepage/category pages render fine once JS executes, unlike Buk/Pobiedziska/Szamotuły (same platform). But its actual WZ register ('Rejestr wydanych decyzji o warunkach zabudowy', m,262) publishes one PDF attachment per calendar year, not one HTML entry per case like Buk's - would require PDF text extraction, a capability this project doesn't have. Its 'Ogłoszenia / Obwieszczenia' category (m,189) defaults to an empty view, but the 'Pokaż archiwalne' (show archived) button IS clickable via Playwright and does reveal real content - however that content is stale/dormant: only 6 entries total, the newest dated 2022-01-19 - over three years old, meaning this channel was abandoned by the municipality, not actively used for current announcements.", null, requiresBrowser = true),
        DiscoverySourceEntry("pobiedziska", "https://bip.pobiedziska.pl", DiscoverySourceStatus.IMPLEMENTED, null, "pobiedziska-komunikaty", requiresBrowser = true),
        DiscoverySourceEntry("szamotuly", "https://bip.szamotuly.pl", DiscoverySourceStatus.IMPLEMENTED, null, "szamotuly-ulicp", requiresBrowser = true),
        DiscoverySourceEntry("komorniki", "https://bip2.komorniki.pl", DiscoverySourceStatus.BLOCKED, "Re-verified: now returns HTTP 403 (was 429) - still a WAF/anti-bot block; archival BIP has a WZ register but is explicitly marked archival.", null),
        DiscoverySourceEntry("lubon", "https://bip.lubon.pl", DiscoverySourceStatus.BLOCKED, "Re-verified: now returns HTTP 403 (was 429) - still a WAF/anti-bot block.", null),
        DiscoverySourceEntry("kostrzyn", "https://bip.kostrzyn.wlkp.pl", DiscoverySourceStatus.BLOCKED, "Re-verified: consistent transport/DNS errors on both HTTP and HTTPS.", null),
        DiscoverySourceEntry("rokietnica", "https://bip.rokietnica.pl", DiscoverySourceStatus.BLOCKED, "Re-verified: consistent transport/DNS errors on both HTTP and HTTPS.", null)
    )

    private val byMunicipality: Map<String, DiscoverySourceEntry> = ALL.associateBy { it.municipalityId }

    fun find(municipalityId: String): DiscoverySourceEntry? = byMunicipality[municipalityId]

    /** Hosts of every entry flagged [DiscoverySourceEntry.requiresBrowser] (see ADR-007). */
    fun browserRequiredHosts(): Set<String> =
        ALL.filter { it.requiresBrowser }
            .mapNotNull { it.bipUrl?.let { url -> runCatching { java.net.URI(url).host }.getOrNull() } }
            .toSet()
}
