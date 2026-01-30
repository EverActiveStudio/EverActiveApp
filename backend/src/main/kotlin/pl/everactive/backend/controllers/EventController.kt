package pl.everactive.backend.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.services.EventService
import pl.everactive.backend.services.RequestService
import pl.everactive.backend.services.RuleEvaluationService
import pl.everactive.shared.ApiResult
import pl.everactive.shared.ApiResult.Error
import pl.everactive.shared.ApiResult.Success
import pl.everactive.shared.ApiRoutes
import pl.everactive.shared.PushEventsRequest
import pl.everactive.shared.PushEventsResponse

@RestController
class EventController(
    private val eventService: EventService,
    private val ruleEvaluationService: RuleEvaluationService,
    private val requestService: RequestService,
) {
    @PostMapping(ApiRoutes.User.EVENTS)
    suspend fun pushEvents(@RequestBody request: PushEventsRequest): ResponseEntity<ApiResult<PushEventsResponse>> {
        val user = requestService.userId
        val userId = checkNotNull(user.id)

        return when (val result = eventService.pushEvents(request, user)) {
            EventService.PushEventsResult.Success -> {
                val response = PushEventsResponse(
                    triggeredRules = ruleEvaluationService.getTriggeredRulesForCurrentUser(userId),
                )

                ResponseEntity.ok(Success(response))
            }

            is EventService.PushEventsResult.Failure -> {
                ResponseEntity.badRequest()
                    .body(Error(Error.Type.Validation, result.reason))
            }
        }
    }
}
