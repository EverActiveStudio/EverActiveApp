package pl.everactive.backend.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.services.EventService
import pl.everactive.shared.ApiResult
import pl.everactive.shared.ApiResult.Error
import pl.everactive.shared.ApiResult.Success
import pl.everactive.shared.ApiRoutes
import pl.everactive.shared.PushEventsRequest

@RestController
class EventController(
    private val eventService: EventService,
) {
    @PostMapping(ApiRoutes.User.EVENTS)
    suspend fun pushEvents(@RequestBody request: PushEventsRequest): ResponseEntity<ApiResult<Unit>> {
        return when (val result = eventService.pushEvents(request)) {
            EventService.PushEventsResult.Success -> {
                ResponseEntity.ok(Success(Unit))
            }

            is EventService.PushEventsResult.Failure -> {
                ResponseEntity.badRequest()
                    .body(Error(Error.Type.Validation, result.reason))
            }
        }
    }
}
