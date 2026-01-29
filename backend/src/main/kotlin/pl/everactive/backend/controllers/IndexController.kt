package pl.everactive.backend.controllers

import org.springframework.data.repository.query.Param
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import pl.everactive.backend.config.Role
import pl.everactive.backend.front.IndexPage
import pl.everactive.backend.front.RegisterPage
import pl.everactive.backend.services.UserService


@RestController("/")
class IndexController(private val userService: UserService) {
    @GetMapping
    fun index(@RequestParam(defaultValue = "false") error: Boolean, @RequestParam(defaultValue = "false") registered: Boolean): String =
        IndexPage(error).render().also {
            println("isError: $error")
        }
    @GetMapping("/register")
    fun registerPage(@RequestParam(required = false) error: String?): String =
        RegisterPage(error).render()

    @PostMapping("/register")
    fun performRegister(
        @RequestParam name: String,
        @RequestParam email: String,
        @RequestParam password: String
    ): Any {
        val request = pl.everactive.shared.dtos.RegisterRequest(email, password, name)

        // Jawne przekazanie roli Manager tylko dla tego formularza
        return when (val result = userService.register(request, Role.Manager)) {
            is UserService.RegistrationResult.Success -> RedirectView("/?registered=true")
            is UserService.RegistrationResult.AlreadyExists -> RegisterPage("Użytkownik już istnieje").render()
            is UserService.RegistrationResult.Failure -> RegisterPage(result.reason).render()
        }
    }
}
