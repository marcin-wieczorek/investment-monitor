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
    val adapterSourceId: String?
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
        DiscoverySourceEntry("kornik", "https://bip.kornik.pl/planowanie-przestrzenne", DiscoverySourceStatus.NOT_IMPLEMENTED, "Planning page exists but obwieszczenia/WZ listing URLs return 404 on the current Drupal 11 site; needs further URL discovery.", null),
        DiscoverySourceEntry("srem", "http://bip.srem.pl/public/?id=192668", DiscoverySourceStatus.NOT_IMPLEMENTED, "Planning section only contains application forms/instructions, no register of issued decisions.", null),
        DiscoverySourceEntry("mosina", "https://bip.mosina.pl", DiscoverySourceStatus.NOT_IMPLEMENTED, "BIP root page is a near-empty redirect stub; no discoverable WZ register.", null),
        DiscoverySourceEntry("murowana_goslina", "https://bip.murowana-goslina.pl", DiscoverySourceStatus.NOT_IMPLEMENTED, "Has Obwieszczenia/Gospodarka sections but no confirmed WZ register structure yet.", null),
        DiscoverySourceEntry("puszczykowo", "https://bip.puszczykowo.pl", DiscoverySourceStatus.NOT_IMPLEMENTED, "SSR site with a Gospodarka przestrzenna section but no confirmed WZ register structure yet.", null),
        DiscoverySourceEntry("kleszczewo", "https://bip.kleszczewo.pl", DiscoverySourceStatus.BLOCKED, "Nefeni (nowoczesnagmina.pl) JavaScript SPA - no server-rendered content.", null),
        DiscoverySourceEntry("dopiewo", "https://bip.dopiewo.pl", DiscoverySourceStatus.BLOCKED, "Nefeni JavaScript SPA - no server-rendered content.", null),
        DiscoverySourceEntry("buk", "https://bip.buk.gmina.pl", DiscoverySourceStatus.BLOCKED, "Nefeni-type JavaScript SPA - no server-rendered content.", null),
        DiscoverySourceEntry("oborniki", "https://bip.oborniki.pl", DiscoverySourceStatus.BLOCKED, "Nefeni-type JavaScript SPA - no server-rendered content.", null),
        DiscoverySourceEntry("pobiedziska", "https://bip.pobiedziska.pl", DiscoverySourceStatus.BLOCKED, "Nefeni-type JavaScript SPA - no server-rendered content.", null),
        DiscoverySourceEntry("szamotuly", "https://bip.szamotuly.pl", DiscoverySourceStatus.BLOCKED, "Nefeni-type JavaScript SPA - no server-rendered content.", null),
        DiscoverySourceEntry("skoki", "https://skoki.nowoczesnagmina.pl", DiscoverySourceStatus.BLOCKED, "Nefeni JavaScript SPA - no server-rendered content.", null),
        DiscoverySourceEntry("komorniki", "https://bip2.komorniki.pl", DiscoverySourceStatus.BLOCKED, "New BIP returns HTTP 429 (WAF/rate limiting); archival BIP has a WZ register but is explicitly marked archival.", null),
        DiscoverySourceEntry("lubon", "https://bip.lubon.pl", DiscoverySourceStatus.BLOCKED, "Returns HTTP 429 (rate limiting/WAF).", null),
        DiscoverySourceEntry("kostrzyn", "https://bip.kostrzyn.wlkp.pl", DiscoverySourceStatus.BLOCKED, "Consistent transport errors on HTTP and HTTPS.", null),
        DiscoverySourceEntry("rokietnica", "https://bip.rokietnica.pl", DiscoverySourceStatus.BLOCKED, "Consistent transport errors on HTTP and HTTPS.", null),
        DiscoverySourceEntry("steszew", "http://bip.steszew.pl", DiscoverySourceStatus.BLOCKED, "Only an archival BIP is reachable; the current BIP URL returns transport errors.", null)
    )

    private val byMunicipality: Map<String, DiscoverySourceEntry> = ALL.associateBy { it.municipalityId }

    fun find(municipalityId: String): DiscoverySourceEntry? = byMunicipality[municipalityId]
}
