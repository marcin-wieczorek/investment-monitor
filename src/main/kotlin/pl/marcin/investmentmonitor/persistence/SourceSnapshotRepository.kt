package pl.marcin.investmentmonitor.persistence

import java.time.Instant

data class SourceSnapshot(
    val source: String,
    val capturedAt: Instant,
    val investmentCount: Int,
    val contentHash: String
)

interface SourceSnapshotRepository {
    fun find(source: String): SourceSnapshot?
    fun save(snapshot: SourceSnapshot)
}
