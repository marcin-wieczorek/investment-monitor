package pl.marcin.investmentmonitor.domain

import java.net.URI
import java.util.Locale

data class Investment(
    val source: String,
    val developer: String,
    val name: String,
    val url: URI,
    val location: String?,
    val propertyType: PropertyType?,
    val units: Int?,
    val houseArea: AreaRange?,
    val plotArea: AreaRange?,
    val price: PriceRange?,
    val status: InvestmentStatus?
) {
    val canonicalKey: String
        get() {
            val normalizedUrl = url.normalize()
                .toString()
                .removeSuffix("/")
                .lowercase(Locale.ROOT)
            return "$source:$normalizedUrl"
        }
}

data class AreaRange(val min: Double?, val max: Double?)
data class PriceRange(val min: Int?, val max: Int?)

enum class PropertyType { TERRACED, SEMI_DETACHED, DETACHED, APARTMENT, UNKNOWN }
enum class InvestmentStatus { PRE_SALE, FOR_SALE, SOLD_OUT, UNKNOWN }
