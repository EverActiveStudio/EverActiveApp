package pl.everactive.backend.services

import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import pl.everactive.backend.entities.GroupEntity
import pl.everactive.backend.entities.RuleEntity
import pl.everactive.backend.entities.RuleEventEntity
import pl.everactive.backend.entities.UserEntity
import pl.everactive.backend.repositories.GroupRepository
import pl.everactive.backend.repositories.RuleEventRepository
import pl.everactive.backend.utils.getLogger
import pl.everactive.shared.Rule
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

// :c

@Service
class RuleEvaluationService(
    private val groupRepository: GroupRepository,
    private val userStateService: UserStateService,
    private val ruleEventRepository: RuleEventRepository,
) {
    private val log = getLogger()
    private val lastEvaluationResults = ConcurrentHashMap<Pair<Long, Long>, Boolean>()

    // hack :c
    private val evaluationResultsPerUser = ConcurrentHashMap<Long, List<Pair<RuleEntity, Boolean>>>()

    @Scheduled(fixedRate = 5_000)
    @Async
    @Transactional
    fun evaluateAllRules() {
        val currentTime = LocalDateTime.now()
        val groups = groupRepository.findAll()
        val events = mutableListOf<RuleEventEntity>()

        for (group in groups) {
            if (!shouldBeEvaluated(group, currentTime)) {
                continue
            }

            group.users.forEach { user ->
                val state = userStateService.getStateSnapshot(user)

                evaluationResultsPerUser[user.id!!] = group.rules.map { rule ->
                    val result = evaluate(rule.rule, state)

                    runCatching {
                        handleEvaluationResult(rule, user, result, currentTime, events)
                    }

                    rule to result
                }
            }
        }

        if (events.isNotEmpty()) {
            try {
                ruleEventRepository.saveAll(events)
            } catch (e: Exception) {
                log.error("Failed to save rule events", e)
            }
        }
    }

    fun getTriggeredRulesForCurrentUser(userId: Long): List<Rule> {
        return evaluationResultsPerUser[userId]
            ?.filter { it.second }
            ?.map { it.first.rule }
            ?: emptyList()
    }

    private fun shouldBeEvaluated(group: GroupEntity, currentTime: LocalDateTime): Boolean {
        return group.timeFrames.any {
            it.contains(currentTime.dayOfWeek.ordinal, currentTime.hour)
        }
    }

    private fun evaluate(rule: Rule, state: UserStateService.State): Boolean {
        return when (rule) {
            is Rule.NotMoved -> {
                val lastMoveTime = state.lastMoveTime ?: return false
                val durationSinceLastMove = Duration.between(lastMoveTime, LocalDateTime.now())

                durationSinceLastMove.toMinutes() >= rule.durationMinutes
            }

            is Rule.MissingUpdates -> {
                val lastEventTime = state.lastEventTime ?: LocalDateTime.MIN
                val durationSinceLastEvent = Duration.between(lastEventTime, LocalDateTime.now())

                durationSinceLastEvent.toMinutes() >= rule.durationMinutes
            }

            is Rule.GeofenceBox -> {
                val lastLocation = state.lastLocation ?: return false

                val latitude = lastLocation.latitude
                val longitude = lastLocation.longitude

                latitude < rule.minLatitude || latitude > rule.maxLatitude ||
                    longitude < rule.minLongitude || longitude > rule.maxLongitude
            }
        }
    }

    private fun handleEvaluationResult(
        rule: RuleEntity,
        user: UserEntity,
        result: Boolean,
        timestamp: LocalDateTime,
        events: MutableList<RuleEventEntity>
    ) {
        val key = checkNotNull(rule.id) to checkNotNull(user.id)

        val previousResult = lastEvaluationResults[key]

        if (previousResult != result) {
            val event = RuleEventEntity(
                rule = rule,
                user = user,
                timestamp = timestamp,
                isFailed = result,
            )
            events.add(event)

            lastEvaluationResults[key] = result
        }
    }
}
