package pl.marcinwieczorek.investmentmonitor.source.discovery

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.marcinwieczorek.investmentmonitor.domain.SignalType
import java.nio.file.Files
import java.nio.file.Path

class SzamotulyUlicpParserTest {

    private val parser = SzamotulyUlicpParser()

    private val listHtml: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/szamotuly-ulicp/list.html"))
    }
    private val article1Html: String by lazy {
        Files.readString(Path.of("src/test/resources/fixtures/szamotuly-ulicp/article-1.html"))
    }

    @Test
    fun `finds every announcement's own article URL from the list page`() {
        val urls = parser.findArticleUrls(listHtml, SzamotulyUlicpSource.LIST_URL)
        urls shouldHaveSize 7
        urls.first() shouldBe "https://bip.szamotuly.pl/a,40129,obwieszczenie-burmistrza-miasta-i-gminy-szamotuly.html"
    }

    @Test
    fun `parses a public-purpose siting announcement from its own article page`() {
        val signal = parser.parseArticle(
            article1Html,
            "https://bip.szamotuly.pl/a,40129,obwieszczenie-burmistrza-miasta-i-gminy-szamotuly.html"
        )

        checkNotNull(signal)
        signal.source shouldBe "szamotuly-ulicp"
        signal.municipality shouldBe "Szamotuły"
        signal.signalType shouldBe SignalType.LAND_DEVELOPMENT_SIGNAL
        // The title names both the village ("Lulinek") and the parent gmina
        // ("Szamotuły", via "Miasta i Gminy Szamotuły") - LocationCatalog.findIn
        // matches whichever it iterates to first; either is a correct
        // location (same accepted ambiguity as MurowanaGoslinaObwieszczeniaParserTest).
        signal.location shouldBe "Szamotuły"
        signal.reference shouldBe "WN.6733.38.2026"
        signal.title shouldBe
            "Obwieszczenie Burmistrza Miasta i Gminy Szamotuły Stosownie do art. 53, ust. 1 ustawy z dnia 27 marca 2003 r. o planowaniu i zagospodarowaniu przestrzennym (t.j. Dz. U. z 2026 r. poz. 538), Burmistrz Miasta i Gminy Szamotuły zawiadamia o wszczęciu postępowania oznaczonego symbolem WN.6733.38.2026 w sprawie ustalenia lokalizacji inwestycji celu publicznego dla inwestycji polegającej na budowie sieci wodociągowej i kanalizacji sanitarnej w celu przyłączenia do sieci wodociągowej i kanalizacji sanitarnej budynków mieszkalnych, na częściach działek o nr ewid. 24/2, 25, 26/62, 26/63, 29/8, 26/42, 26/46, 26/35, 26/34, 26/33, obręb Lulinek, gmina Szamotuły. W związku z powyższym informuję zainteresowanych, że w Wydziale Nieruchomości i Gospodarki Przestrzennej Urzędu Miasta i Gminy Szamotuły, ul. Dworcowa 24, pokój nr 1, od poniedziałku do piątku w godzinach pracy Urzędu można zapoznać się z aktami sprawy."
        signal.url.toString() shouldBe "https://bip.szamotuly.pl/a,40129,obwieszczenie-burmistrza-miasta-i-gminy-szamotuly.html"
        signal.detectedAt.toString() shouldBe "2026-08-18T13:24:13Z"
    }
}
