package pl.marcin.investmentmonitor.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DeveloperNameMatcherTest {

    @Test
    fun `matches a name with and without a Sp z o o suffix`() {
        DeveloperNameMatcher.matches("ABC Development", "ABC Development Sp. z o.o.") shouldBe true
    }

    @Test
    fun `matches regardless of case and extra whitespace`() {
        DeveloperNameMatcher.matches("abc   development", "ABC Development") shouldBe true
    }

    @Test
    fun `matches a name with a spelled-out spolka z ograniczona odpowiedzialnoscia suffix`() {
        DeveloperNameMatcher.matches(
            "ABC Development",
            "ABC Development Spółka z ograniczoną odpowiedzialnością"
        ) shouldBe true
    }

    @Test
    fun `matches S A suffix`() {
        DeveloperNameMatcher.matches("Develia", "Develia S.A.") shouldBe true
    }

    @Test
    fun `matches Sp k suffix`() {
        DeveloperNameMatcher.matches("Chronos Development", "Chronos Development Sp. k.") shouldBe true
    }

    @Test
    fun `does not match genuinely different developers`() {
        DeveloperNameMatcher.matches("ABC Development", "XYZ Development Sp. z o.o.") shouldBe false
    }

    @Test
    fun `does not match a real observed developer-candidate pair from production data`() {
        DeveloperNameMatcher.matches("VIEW DEVELOPMENT 2.0 Sp. z o.o.", "Hermanos s. c.") shouldBe false
    }

    @Test
    fun `matches real observed VIEW DEVELOPMENT variants`() {
        DeveloperNameMatcher.matches("VIEW DEVELOPMENT 2.0 Sp. z o.o.", "VIEW DEVELOPMENT 2.0") shouldBe true
    }
}
