package pl.marcin.investmentmonitor.monitoring

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.reporting.ScanReportRenderer

/**
 * Runs a single scan on application startup and exits.
 *
 * `./gradlew bootRun` is intentionally one-shot: start, scan, report, stop.
 *
 * Disabled when `investment-monitor.mode=rescore` (see [RescoreRunner]) -
 * that mode only recomputes `investment_score` from already-known facts
 * and must never also trigger a full live-source scan.
 */
@Component
@ConditionalOnProperty(prefix = "investment-monitor", name = ["mode"], havingValue = "scan", matchIfMissing = true)
class ScanRunner(private val monitoringService: MonitoringService) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val report = monitoringService.scan()
        logger.info("\n{}", ScanReportRenderer.render(report))
    }

    private companion object {
        val logger = LoggerFactory.getLogger(ScanRunner::class.java)
    }
}
