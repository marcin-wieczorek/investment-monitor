package pl.marcinwieczorek.investmentmonitor.analysis

import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.LocationCatalog
import pl.marcinwieczorek.investmentmonitor.domain.LocationProfile

/** Resolves the curated [LocationProfile] for an investment's free-text [Investment.location], if recognized. */
fun locationProfileFor(investment: Investment): LocationProfile? =
    investment.location?.let(LocationCatalog::findIn)?.let(LocationProfiles::find)
