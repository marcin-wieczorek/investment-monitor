package pl.marcinwieczorek.investmentmonitor.registry

import pl.marcinwieczorek.investmentmonitor.domain.Developer
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperNameMatcher
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperStatus
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperTier
import java.net.URI

/**
 * Explicit, reviewable registry of every developer the system tracks -
 * Tier A/B priority lists from AGENTS.md sections 3/4, mirrored into the
 * `developer_registry` table by `V5__developer_municipality_registry.sql`.
 *
 * A developer never disappears from this list just because it has no
 * adapter or no current investments (see [DeveloperStatus]). Do not invent
 * [Developer.website] values - only verified URLs are recorded.
 */
object DeveloperRegistry {

    val ALL: List<Developer> = listOf(
        // Tier A
        developer("chronos", "Chronos Development", "https://www.chronos.poznan.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Komorniki", "Swarzędz", "Kruszewnia", "Rokietnica"), "chronos"),
        developer("greenbud", "Greenbud Development", "https://www.greenbud.com.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Swarzędz", "Pobiedziska"), "greenbud"),
        developer("jakon", "Jakon", "https://www.jakon-inwest.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań", "Tarnowo Podgórne", "Mosina"), "jakon-inwest"),
        developer("nickel", "Nickel Development", "https://www.nickel.com.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "nickel"),
        developer("agrobex", "Agrobex", "https://www.agrobex.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań", "Kleszczewo", "Pobiedziska", "Szamotuły", "Śrem"), "agrobex"),
        developer("linea", "Linea", "https://linea-deweloper.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Dopiewo", "Murowana Goślina", "Buk"), "linea"),
        developer("duda", "Duda Development", "https://dudadevelopment.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "duda"),
        developer("ataner", "Ataner", "https://www.ataner.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "ataner"),
        developer("uwi", "UWI", "https://uwi.com.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "uwi"),
        developer("pwd", "PWD Deweloper", "https://pwd-mieszkania.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "pwd", requiresBrowser = true),
        developer("villa", "Villa", null, DeveloperTier.A, DeveloperStatus.CANDIDATE, emptySet(), null),
        developer("konimpex", "Konimpex-Invest", "https://www.konimpex-invest.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "konimpex"),
        developer("sovo", "Sovo Development", null, DeveloperTier.A, DeveloperStatus.BLOCKED, emptySet(), null),
        developer("pekabex", "Pekabex Development", "https://pekabexdevelopment.com", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "pekabex"),
        developer("monday", "Monday Development", "https://mondaydevelopment.pl", DeveloperTier.A, DeveloperStatus.NO_CURRENT_INVESTMENTS, setOf("Poznań"), null),
        developer("murapol", "Murapol", "https://murapol.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "murapol"),
        developer("develia", "Develia", "https://develia.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "develia"),
        developer("atal", "ATAL", "https://atal.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań", "Swarzędz"), "atal"),
        developer("archicom", "Archicom / Echo Residential", "https://archicom.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "archicom", requiresBrowser = true),
        developer("robyg", "ROBYG", "https://robyg.pl", DeveloperTier.A, DeveloperStatus.MONITORED, setOf("Poznań"), "robyg"),

        // Tier B
        developer("ebf", "EBF Development", "https://ebfdevelopment.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "ebf"),
        developer("cordia", "Cordia", "https://cordiapolska.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "cordia"),
        developer("ronson", "Ronson", "https://ronson.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "ronson"),
        developer("budimex", "Budimex", null, DeveloperTier.B, DeveloperStatus.INACTIVE, emptySet(), null),
        developer("novaform", "Novaform", null, DeveloperTier.B, DeveloperStatus.BLOCKED, emptySet(), null),
        developer("ggw", "GGW Development", "https://ggwdevelopment.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "ggw"),
        developer("sivanet", "SIVANET", "https://sivanet.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "sivanet"),
        developer("mj", "MJ Deweloper", "https://mjdeweloper.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "mj"),
        developer("spravia", "Spravia", "https://spravia.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "spravia"),
        developer("cavallia", "Cavallia", null, DeveloperTier.B, DeveloperStatus.BLOCKED, emptySet(), null),
        developer("area", "Area Development", "https://areadevelopment.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "area"),
        developer("jaksbud", "JakśBud", "https://jaksbud.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "jaksbud"),
        developer("btm", "BTM", null, DeveloperTier.B, DeveloperStatus.BLOCKED, emptySet(), null),
        developer("constructa_plus", "Constructa Plus", null, DeveloperTier.B, DeveloperStatus.BLOCKED, emptySet(), null),
        developer("inwestycje_wielkopolski", "Inwestycje Wielkopolski", "https://inwestycjewielkopolski.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "inwestycje_wielkopolski"),
        developer("virke", "Virke", null, DeveloperTier.B, DeveloperStatus.BLOCKED, emptySet(), null),
        developer("sgi", "SGI", "https://sgi.pl", DeveloperTier.B, DeveloperStatus.BLOCKED, emptySet(), null),
        developer("sagaris", "Sagaris", "https://sagaris.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "sagaris"),
        developer("vastbouw", "Vastbouw", "https://vastbouw.pl", DeveloperTier.B, DeveloperStatus.MONITORED, setOf("Poznań"), "vastbouw"),
        developer("fb_antczak", "FB Antczak", null, DeveloperTier.B, DeveloperStatus.BLOCKED, emptySet(), null)
    )

    private val byNormalizedName: Map<String, Developer> = ALL.associateBy { DeveloperNameMatcher.normalize(it.name) }

    /** Looks up a registered developer by (fuzzy, legal-suffix/case/whitespace-insensitive) name match. */
    fun findByName(name: String): Developer? = byNormalizedName[DeveloperNameMatcher.normalize(name)]

    fun find(id: String): Developer? = ALL.firstOrNull { it.id == id }

    /** Hosts of every developer flagged [Developer.requiresBrowser] (see ADR-007). */
    fun browserRequiredHosts(): Set<String> =
        ALL.filter { it.requiresBrowser }
            .mapNotNull { it.website?.host }
            .toSet()

    private fun developer(
        id: String,
        name: String,
        website: String?,
        tier: DeveloperTier,
        status: DeveloperStatus,
        geographicScope: Set<String>,
        adapterSourceId: String?,
        requiresBrowser: Boolean = false
    ): Developer = Developer(
        id = id,
        name = name,
        website = website?.let(::URI),
        investmentListUrls = emptyList(),
        tier = tier,
        status = status,
        geographicScope = geographicScope,
        adapterSourceId = adapterSourceId,
        requiresBrowser = requiresBrowser
    )
}
