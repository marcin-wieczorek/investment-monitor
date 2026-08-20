package pl.marcinwieczorek.investmentmonitor.domain

data class LocationProfile(
    val name: String,
    val tier: DevelopmentTier,
    val growthScore: Int,
    val infrastructureScore: Int,
    val transportScore: Int,
    val familyScore: Int
) {
    init {
        val scores = listOf(growthScore, infrastructureScore, transportScore, familyScore)
        scores.forEach { score ->
            require(score in 0..10) { "Score must be in 0..10, got $score" }
        }
    }
}

enum class DevelopmentTier { S, A, B }
