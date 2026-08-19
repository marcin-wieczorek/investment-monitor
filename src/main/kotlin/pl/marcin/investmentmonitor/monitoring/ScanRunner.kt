package pl.marcin.investmentmonitor.monitoring

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import pl.marcin.investmentmonitor.reporting.ScanReportRenderer

/**
 * Runs a single scan on application startup and exits.
 *
 * `./gradlew bootRun` is intentionally one-shot: start, scan, report, stop.
 */
@Component
class ScanRunner(private val monitoringService: MonitoringService) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val report = monitoringService.scan()
        println(ScanReportRenderer.render(report))
    }
}
