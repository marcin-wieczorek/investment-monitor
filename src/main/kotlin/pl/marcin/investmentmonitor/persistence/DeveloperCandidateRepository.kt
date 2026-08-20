package pl.marcin.investmentmonitor.persistence

import pl.marcin.investmentmonitor.domain.DeveloperCandidate
import pl.marcin.investmentmonitor.domain.DeveloperCandidateStatus

interface DeveloperCandidateRepository {
    fun save(candidate: DeveloperCandidate): Long
    fun findAll(): List<DeveloperCandidate>
    fun findByName(developerName: String): DeveloperCandidate?
    fun updateStatus(id: Long, status: DeveloperCandidateStatus)
}
