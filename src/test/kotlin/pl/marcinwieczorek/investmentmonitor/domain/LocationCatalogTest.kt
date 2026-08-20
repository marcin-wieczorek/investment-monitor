package pl.marcinwieczorek.investmentmonitor.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LocationCatalogTest {

    @Test
    fun `finds an exact known location name`() {
        LocationCatalog.findIn("Kruszewnia") shouldBe "Kruszewnia"
    }

    @Test
    fun `finds a known location as a substring surrounded by other text`() {
        LocationCatalog.findIn("Nowa inwestycja w miejscowości Rabowice, w pobliżu lasu") shouldBe "Rabowice"
    }

    @Test
    fun `finds a location immediately followed by punctuation despite a Polish diacritic at the boundary`() {
        // Regression test for the documented \b pitfall: Java's \b treats
        // "ń" as a non-word character, so a naive \bPoznań\b would not
        // match "Poznań," - findIn must handle this correctly.
        LocationCatalog.findIn("Inwestycja w Poznań, ul. Głogowska") shouldBe "Poznań"
    }

    @Test
    fun `returns null when no known location is mentioned`() {
        LocationCatalog.findIn("Somewhere entirely unrelated") shouldBe null
    }

    @Test
    fun `is case-insensitive`() {
        LocationCatalog.findIn("KRUSZEWNIA") shouldBe "Kruszewnia"
    }

    @Test
    fun `does not match a location name embedded inside a longer word`() {
        // "Buk" must not match inside an unrelated longer word like "Bukiety".
        LocationCatalog.findIn("Bukiety kwiatowe na sprzedaż") shouldBe null
    }

    @Test
    fun `ALL_LOCATIONS is the union of every gmina-specific set`() {
        LocationCatalog.ALL_LOCATIONS shouldBe (
            LocationCatalog.CORE_LOCATIONS + LocationCatalog.SWARZEDZ_GMINA_VILLAGES +
                LocationCatalog.SREM_GMINA_VILLAGES + LocationCatalog.MUROWANA_GOSLINA_GMINA_VILLAGES +
                LocationCatalog.BUK_GMINA_VILLAGES + LocationCatalog.SZAMOTULY_GMINA_VILLAGES +
                LocationCatalog.POBIEDZISKA_GMINA_VILLAGES + LocationCatalog.KORNIK_GMINA_VILLAGES +
                LocationCatalog.DOPIEWO_GMINA_VILLAGES
            )
    }
}
