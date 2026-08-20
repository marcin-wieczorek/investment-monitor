package pl.marcinwieczorek.investmentmonitor.persistence

import pl.marcinwieczorek.investmentmonitor.domain.DeveloperCandidate
import pl.marcinwieczorek.investmentmonitor.domain.DeveloperCandidateStatus

interface DeveloperCandidateRepository {
    fun save(candidate: DeveloperCandidate): Long
    fun findAll(): List<DeveloperCandidate>
    fun findByName(developerName: String): DeveloperCandidate?
    fun updateStatus(id: Long, status: DeveloperCandidateStatus)
}
