package pl.marcinwieczorek.investmentmonitor.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class JdbcLlmAnalysisRepository(private val jdbcTemplate: JdbcTemplate) : LlmAnalysisRepository {

    override fun findCached(investmentCanonicalKey: String, model: String, promptHash: String): String? =
        jdbcTemplate.query(SELECT, { rs, _ -> rs.getString("response") }, investmentCanonicalKey, model, promptHash)
            .firstOrNull()

    override fun save(investmentCanonicalKey: String, model: String, promptHash: String, responseJson: String) {
        jdbcTemplate.update(INSERT, investmentCanonicalKey, model, promptHash, responseJson, Instant.now().toString())
    }

    private companion object {
        const val SELECT =
            "SELECT response FROM llm_analysis WHERE investment_canonical_key = ? AND model = ? AND prompt_hash = ?"
        const val INSERT = """
            INSERT INTO llm_analysis (investment_canonical_key, model, prompt_hash, response, analyzed_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(investment_canonical_key, prompt_hash) DO UPDATE SET response = excluded.response, analyzed_at = excluded.analyzed_at
        """
    }
}
