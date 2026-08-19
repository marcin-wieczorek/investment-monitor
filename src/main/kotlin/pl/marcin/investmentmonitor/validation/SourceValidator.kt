package pl.marcin.investmentmonitor.validation

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.domain.Investment

data class ValidationResult(val valid: Boolean, val reason: String? = null)

@Component
class SourceValidator(
    @param:Value("\${investment-monitor.validation.max-investment-drop-percentage:50}")
    private val maxInvestmentDropPercentage: Int = 50
) {
    fun validate(current: List<Investment>, previousCount: Int?): ValidationResult {
        if (current.any { it.name.isBlank() || it.url.toString().isBlank() }) {
            return ValidationResult(false, "At least one investment has an invalid identity.")
        }
        if (previousCount != null && previousCount > 0) {
            val drop = ((previousCount - current.size).coerceAtLeast(0) * 100) / previousCount
            if (drop > maxInvestmentDropPercentage) {
                return ValidationResult(false, "Investment count dropped from $previousCount to ${current.size} ($drop%).")
            }
        }
        return ValidationResult(true)
    }
}
