package pl.marcinwieczorek.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class SuchyLasNppParserTest {

    private val parser = SuchyLasNppParser()

    private val fixtureHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/suchy-las-npp/announcements.html"))
    }

    @Test
    fun `parses every published announcement`() {
        val signals = parser.parse(fixtureHtml)
        signals shouldHaveSize 10
    }

    @Test
    fun `parses an MPZP announcement about Biedrusko`() {
        val signal = parser.parse(fixtureHtml).first { it.title.contains("Biedrusko") }

        signal.source shouldBe "suchy-las-npp"
        signal.municipality shouldBe "Suchy Las"
        signal.signalType shouldBe SignalType.MPZP_CHANGE
        signal.reference shouldBe null
    }
}
