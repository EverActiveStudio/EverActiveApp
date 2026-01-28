package pl.everactive.backend.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.front.IndexPage

@RestController
class IndexController {
    @GetMapping
    fun index(): String = IndexPage().render()
}
