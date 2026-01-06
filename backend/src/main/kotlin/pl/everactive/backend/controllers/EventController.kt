package pl.everactive.backend.controllers

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pl.everactive.shared.PushEventsRequest

@RestController("/api/events")
class EventController {
    @PostMapping("/push")
    fun pushEvents(@RequestBody request: PushEventsRequest) {
        TODO()
    }
}
