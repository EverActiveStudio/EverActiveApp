package pl.everactive.backend.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.front.IndexPage
import pl.everactive.backend.services.UserService


@RestController("/")
class IndexController(private val userService: UserService) {
    @GetMapping
    fun index(@RequestParam(defaultValue = "false") error: Boolean): String =
        IndexPage(error).render()
}
