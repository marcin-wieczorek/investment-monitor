package pl.marcinwieczorek.investmentmonitor.source.discovery

import pl.marcinwieczorek.investmentmonitor.domain.SignalType

/**
 * Shared classification of a BIP obwieszczenia/komunikaty title into a
 * [SignalType], by keyword. Extracted from four near-identical
 * private `toSignalType()` copies in [BukObwieszczeniaParser],
 * [SzamotulyUlicpParser], [PobiedziskaKomunikatyParser] and
 * [MurowanaGoslinaObwieszczeniaParser] (see docs review - "toSignalType
 * duplicated across 4 discovery parsers" finding). [KornikObwieszczeniaParser]
 * previously had a reduced variant missing the MPZP branch entirely - using
 * this shared classifier there only ever reclassifies a title that would
 * otherwise have silently fallen through to [SignalType.OTHER], never the
 * reverse, so it is a strict improvement, not a behavior change for any
 * title actually observed in that source's fixtures.
 *
 * Each site is still free to diverge from this shared classifier (e.g. a
 * site using genuinely different terminology) by simply not calling it -
 * this is a "genuinely shared, verified" utility, not a mandatory contract.
 */
object SignalTypeClassifier {

    fun fromTitle(title: String): SignalType = when {
        title.contains("warunkach zabudowy", ignoreCase = true) ||
            title.contains("warunków zabudowy", ignoreCase = true) -> SignalType.WZ_DECISION
        title.contains("celu publicznego", ignoreCase = true) -> SignalType.LAND_DEVELOPMENT_SIGNAL
        title.contains("planu zagospodarowania", ignoreCase = true) ||
            title.contains("planu miejscowego", ignoreCase = true) ||
            title.contains("planu ogólnego", ignoreCase = true) -> SignalType.MPZP_CHANGE
        else -> SignalType.OTHER
    }
}
