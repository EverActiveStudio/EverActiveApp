package pl.everactive.backend.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.everactive.backend.entities.RuleEventEntity

@Repository
interface RuleEventRepository : JpaRepository<RuleEventEntity, Long>
