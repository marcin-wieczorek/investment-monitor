package pl.marcinwieczorek.investmentmonitor.domain

/**
 * Whether a given source category currently covers a municipality.
 *
 * A municipality without an implemented source stays visible as
 * [NOT_IMPLEMENTED] or [BLOCKED] rather than silently disappearing from
 * the system - see AGENTS.md geographic coverage section.
 */
enum class MunicipalitySourceStatus {
    /** A working source adapter covers this municipality. */
    IMPLEMENTED,

    /** No adapter yet, no known blocker. */
    NOT_IMPLEMENTED,

    /** Investigated, found technically unreachable (JS SPA, WAF, no register, ...). */
    BLOCKED,

    /** Was implemented, deliberately turned off. */
    DISABLED
}

/**
 * A municipality within the Poznań metropolitan area target scope, as a
 * first-class domain concept independent of which sources cover it.
 *
 * [developerCoverage]/[discoveryCoverage]/[aggregatorCoverage] make blind
 * spots explicit per source category (see AGENTS.md section 29).
 */
data class Municipality(
    val id: String,
    val name: String,
    val powiat: String,
    val developerCoverage: MunicipalitySourceStatus,
    val discoveryCoverage: MunicipalitySourceStatus,
    val aggregatorCoverage: MunicipalitySourceStatus
) {
    init {
        require(id.isNotBlank()) { "Municipality id must not be blank" }
        require(name.isNotBlank()) { "Municipality name must not be blank" }
    }
}
