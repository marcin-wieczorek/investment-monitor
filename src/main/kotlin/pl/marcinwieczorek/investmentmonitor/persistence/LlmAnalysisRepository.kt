package pl.marcinwieczorek.investmentmonitor.persistence

interface LlmAnalysisRepository {
    /** Returns the cached raw JSON response for this exact investment+prompt, if any. */
    fun findCached(investmentCanonicalKey: String, model: String, promptHash: String): String?
    fun save(investmentCanonicalKey: String, model: String, promptHash: String, responseJson: String)
}
