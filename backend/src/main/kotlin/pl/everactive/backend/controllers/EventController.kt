package pl.everactive.backend.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.services.EventService
import pl.everactive.shared.ErrorDto
import pl.everactive.shared.PushEventsRequest

@RestController
@RequestMapping("/api/event")
class EventController(
    private val eventService: EventService,
) {
    @PostMapping("/push")
    suspend fun pushEvents(@RequestBody request: PushEventsRequest) : ResponseEntity<*> {
        return when (val result = eventService.pushEvents(request)) {
            EventService.PushEventsResult.Success -> {
                ResponseEntity.ok()
                    .build<Any>()
            }

            is EventService.PushEventsResult.Failure -> {
                ResponseEntity.badRequest()
                    .body(ErrorDto(result.reason))
            }
        }
    }
}
