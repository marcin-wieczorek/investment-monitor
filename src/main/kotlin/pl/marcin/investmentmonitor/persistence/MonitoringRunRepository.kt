package pl.marcin.investmentmonitor.persistence

import java.time.Instant

interface MonitoringRunRepository {
    fun start(startedAt: Instant): Long

    fun finish(
        id: Long,
        finishedAt: Instant,
        status: String,
        sourcesChecked: Int,
        sourcesFailed: Int,
        newInvestments: Int
    )
}
