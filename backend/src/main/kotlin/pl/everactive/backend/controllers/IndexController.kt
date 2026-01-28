package pl.everactive.backend.controllers

import org.springframework.data.repository.query.Param
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.front.IndexPage

@RestController("/")
class IndexController {
    @GetMapping
    fun index(@RequestParam(defaultValue = "false") error: Boolean): String =
        IndexPage(error).render().also {
            println("isError: $error")
        }
}
