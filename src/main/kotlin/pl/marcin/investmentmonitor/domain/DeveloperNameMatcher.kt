package pl.marcin.investmentmonitor.domain

/**
 * Fuzzy developer-name matching shared by
 * [pl.marcin.investmentmonitor.registry.DeveloperRegistry.findByName] and
 * [pl.marcin.investmentmonitor.persistence.DeveloperCandidateRepository.findByName].
 *
 * Exact-string matching alone misses obvious duplicates like "ABC
 * Development" vs "ABC Development Sp. z o.o." - the same company named
 * with or without its legal-entity suffix, which is exactly how the same
 * developer tends to appear inconsistently across different aggregator
 * listings. This never invents a match across genuinely different
 * companies: it only strips well-known Polish legal-entity suffixes and
 * normalizes whitespace/case/punctuation before comparing, so "ABC
 * Development" and "XYZ Development" remain distinct.
 */
object DeveloperNameMatcher {

    /**
     * Normalizes a developer name for comparison: lowercases, strips a
     * trailing legal-entity form (Sp. z o.o., S.A., Sp. k., Sp.j., S.C.,
     * spółka z ograniczoną odpowiedzialnością, ...), collapses whitespace
     * and drops trailing punctuation.
     */
    fun normalize(name: String): String {
        var result = name.trim().lowercase()
        result = LEGAL_SUFFIX.replace(result, "")
        result = PUNCTUATION.replace(result, " ")
        result = WHITESPACE.replace(result, " ").trim()
        return result
    }

    /** Whether [a] and [b] refer to the same developer once legal-entity noise is stripped. */
    fun matches(a: String, b: String): Boolean = normalize(a) == normalize(b)

    private val LEGAL_SUFFIX = Regex(
        "\\b(sp(?:ó|o)łka z ograniczon(?:ą|a) odpowiedzialno(?:ś|s)ci(?:ą|a)|" +
            "sp\\.?\\s*z\\s*o\\.?\\s*o\\.?|s\\.?\\s*a\\.?|sp\\.?\\s*k\\.?|sp\\.?\\s*j\\.?|s\\.?\\s*c\\.?|" +
            "spó(?:ł|l)ka\\s+akcyjna|spó(?:ł|l)ka\\s+komandytowa|spó(?:ł|l)ka\\s+jawna|" +
            "spó(?:ł|l)ka\\s+cywilna)\\s*\\.?\\s*$"
    )
    private val PUNCTUATION = Regex("[.,]")
    private val WHITESPACE = Regex("\\s+")
}
