package pl.marcinwieczorek.investmentmonitor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import pl.marcinwieczorek.investmentmonitor.config.InvestmentMonitorProperties

@SpringBootApplication
@EnableConfigurationProperties(InvestmentMonitorProperties::class)
class InvestmentMonitorApplication

fun main(args: Array<String>) {
    runApplication<InvestmentMonitorApplication>(*args)
}
