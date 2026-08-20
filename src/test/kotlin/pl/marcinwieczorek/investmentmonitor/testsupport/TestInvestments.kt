package pl.marcinwieczorek.investmentmonitor.testsupport

import pl.marcinwieczorek.investmentmonitor.domain.AreaRange
import pl.marcinwieczorek.investmentmonitor.domain.Investment
import pl.marcinwieczorek.investmentmonitor.domain.InvestmentStatus
import pl.marcinwieczorek.investmentmonitor.domain.PriceRange
import pl.marcinwieczorek.investmentmonitor.domain.PropertyType
import java.net.URI

/**
 * Builds an [Investment] for tests with sensible defaults, so each test only
 * needs to specify the fields relevant to what it's actually verifying
 * instead of repeating every constructor argument.
 */
fun testInvestment(
    name: String = "Test Investment",
    source: String = "chronos",
    developer: String = "Chronos Development",
    url: URI = URI("https://example.com/$name"),
    location: String? = null,
    propertyType: PropertyType? = null,
    units: Int? = null,
    houseArea: AreaRange? = null,
    plotArea: AreaRange? = null,
    price: PriceRange? = null,
    status: InvestmentStatus? = null,
    imageUrl: String? = null
): Investment = Investment(
    source = source,
    developer = developer,
    name = name,
    url = url,
    location = location,
    propertyType = propertyType,
    units = units,
    houseArea = houseArea,
    plotArea = plotArea,
    price = price,
    status = status,
    imageUrl = imageUrl
)
