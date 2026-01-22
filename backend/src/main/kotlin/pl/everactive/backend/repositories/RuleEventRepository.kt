package pl.everactive.backend.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pl.everactive.backend.entities.RuleEventEntity

@Repository
interface RuleEventRepository : JpaRepository<RuleEventEntity, Long> {
    @Query("""
        SELECT re1.*
        FROM rule_events re1
        JOIN (
            SELECT user_id, rule_id, MAX(timestamp) AS max_timestamp
            FROM rule_events
            GROUP BY user_id, rule_id
        ) re2 ON re1.user_id = re2.user_id AND re1.rule_id = re2.rule_id AND re1.timestamp = re2.max_timestamp
    """, nativeQuery = true)
    fun getLatestForAllUsersAndAllRules(): List<RuleEventEntity>
}
