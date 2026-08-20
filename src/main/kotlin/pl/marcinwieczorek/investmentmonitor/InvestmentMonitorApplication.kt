package pl.marcinwieczorek.investmentmonitor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class InvestmentMonitorApplication

fun main(args: Array<String>) {
    runApplication<InvestmentMonitorApplication>(*args)
}
