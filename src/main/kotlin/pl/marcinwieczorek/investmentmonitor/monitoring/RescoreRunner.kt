package pl.marcinwieczorek.investmentmonitor.monitoring

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Runs a one-shot rescore-all pass on application startup and exits,
 * instead of the normal full scan (see [ScanRunner]).
 *
 * Activated via `--investment-monitor.mode=rescore` (the frontend
 * `/api/rescore` route invokes `./gradlew bootRun` with that argument -
 * see `frontend/app/api/rescore/route.ts`), so a user changing scoring
 * preferences in `/settings` gets updated `investment_score` values
 * without waiting for (or triggering) a full live-source scan.
 */
@Component
@ConditionalOnProperty(prefix = "investment-monitor", name = ["mode"], havingValue = "rescore")
class RescoreRunner(private val rescoreService: RescoreService) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val count = rescoreService.rescoreAll()
        logger.info("Rescore complete: {} investment(s) recomputed", count)
    }

    private companion object {
        val logger = LoggerFactory.getLogger(RescoreRunner::class.java)
    }
}
