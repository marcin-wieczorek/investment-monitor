package pl.marcinwieczorek.investmentmonitor.persistence

import pl.marcinwieczorek.investmentmonitor.domain.SourceCategory
import java.time.Instant

data class SourceSnapshot(
    val source: String,
    val capturedAt: Instant,
    val investmentCount: Int,
    /**
     * A fingerprint of *which* canonical keys were present at capture time,
     * not a hash of their field contents. Field-level changes are detected
     * separately and precisely by ChangeDetector; this is a cheap identity
     * check, not a content-staleness check.
     */
    val contentHash: String,
    val sourceCategory: SourceCategory = SourceCategory.DEVELOPER
)

interface SourceSnapshotRepository {
    fun find(source: String): SourceSnapshot?
    fun save(snapshot: SourceSnapshot)
}
