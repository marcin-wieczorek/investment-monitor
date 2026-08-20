package pl.marcinwieczorek.investmentmonitor.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import pl.marcinwieczorek.investmentmonitor.domain.ReferenceInvestmentProfile
import java.time.Instant

@Repository
class JdbcUserPreferencesRepository(private val jdbcTemplate: JdbcTemplate) : UserPreferencesRepository {

    override fun findScoringProfile(): ReferenceInvestmentProfile? =
        jdbcTemplate.query(SELECT, { rs, _ -> rs.getString("value") }, SCORING_PROFILE_KEY)
            .firstOrNull()
            ?.let { MAPPER.readValue(it) }

    override fun saveScoringProfile(profile: ReferenceInvestmentProfile) {
        jdbcTemplate.update(UPSERT, SCORING_PROFILE_KEY, MAPPER.writeValueAsString(profile), Instant.now().toString())
    }

    private companion object {
        val MAPPER = jacksonObjectMapper()
        const val SCORING_PROFILE_KEY = "scoring.profile"

        const val SELECT = "SELECT value FROM user_preferences WHERE key = ?"
        const val UPSERT = """
            INSERT INTO user_preferences (key, value, updated_at)
            VALUES (?, ?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at
        """
    }
}
