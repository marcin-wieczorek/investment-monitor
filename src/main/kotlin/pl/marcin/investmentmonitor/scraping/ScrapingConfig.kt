package pl.marcin.investmentmonitor.scraping

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.marcin.investmentmonitor.registry.DeveloperRegistry
import pl.marcin.investmentmonitor.registry.DiscoverySourceRegistry

/**
 * Wires together the set of hosts that require a headless-browser fetcher
 * (see ADR-007), derived from the existing source registries rather than
 * new YAML config - consistent with `AGENTS.md`'s "source knowledge lives
 * entirely in Kotlin code" convention. [ArchivingPageFetcher] consumes
 * this to decide, per fetch, whether to delegate to [JsoupPageFetcher] or
 * [PlaywrightPageFetcher].
 */
@Configuration
class ScrapingConfig {

    @Bean
    fun browserRequiredHosts(): Set<String> =
        DiscoverySourceRegistry.browserRequiredHosts() + DeveloperRegistry.browserRequiredHosts()
}
