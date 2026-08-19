package pl.marcin.investmentmonitor.persistence

import java.time.Instant

enum class RunStatus { RUNNING, SUCCESS, PARTIAL_FAILURE }

interface MonitoringRunRepository {
    fun start(startedAt: Instant): Long

    fun finish(
        id: Long,
        finishedAt: Instant,
        status: RunStatus,
        sourcesChecked: Int,
        sourcesFailed: Int,
        newInvestments: Int
    )
}
