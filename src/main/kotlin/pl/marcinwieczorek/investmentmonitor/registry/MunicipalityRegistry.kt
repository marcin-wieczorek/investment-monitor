package pl.marcinwieczorek.investmentmonitor.registry

import pl.marcinwieczorek.investmentmonitor.domain.Municipality
import pl.marcinwieczorek.investmentmonitor.domain.MunicipalitySourceStatus

/**
 * Explicit registry of all 23 target Metropolia Poznań municipalities
 * (AGENTS.md section 1), mirrored into the `municipality_registry` table
 * by `V5__developer_municipality_registry.sql`.
 *
 * A municipality never disappears from this list for lack of source
 * coverage - see [MunicipalitySourceStatus].
 */
object MunicipalityRegistry {

    val ALL: List<Municipality> = listOf(
        m("poznan", "Poznań", "miasto na prawach powiatu", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED),
        m("buk", "Buk", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("czerwonak", "Czerwonak", "poznański", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("dopiewo", "Dopiewo", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("kleszczewo", "Kleszczewo", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("komorniki", "Komorniki", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.BLOCKED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("kostrzyn", "Kostrzyn", "poznański", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.BLOCKED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("kornik", "Kórnik", "poznański", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("lubon", "Luboń", "poznański", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.BLOCKED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("mosina", "Mosina", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("murowana_goslina", "Murowana Goślina", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("oborniki", "Oborniki", "obornicki", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("pobiedziska", "Pobiedziska", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("puszczykowo", "Puszczykowo", "poznański", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("rokietnica", "Rokietnica", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.BLOCKED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("skoki", "Skoki", "wągrowiecki", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("steszew", "Stęszew", "poznański", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("suchy_las", "Suchy Las", "poznański", MunicipalitySourceStatus.NOT_IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("swarzedz", "Swarzędz", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED),
        m("szamotuly", "Szamotuły", "szamotulski", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("srem", "Śrem", "śremski", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED),
        m("tarnowo_podgorne", "Tarnowo Podgórne", "poznański", MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.IMPLEMENTED, MunicipalitySourceStatus.NOT_IMPLEMENTED)
    )

    private val byId: Map<String, Municipality> = ALL.associateBy { it.id }

    fun find(id: String): Municipality? = byId[id]

    private fun m(
        id: String,
        name: String,
        powiat: String,
        developerCoverage: MunicipalitySourceStatus,
        discoveryCoverage: MunicipalitySourceStatus,
        aggregatorCoverage: MunicipalitySourceStatus
    ): Municipality = Municipality(id, name, powiat, developerCoverage, discoveryCoverage, aggregatorCoverage)
}
