package pl.everactive.backend.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.security.HasManagerRole
import pl.everactive.backend.services.UserService
import pl.everactive.shared.ApiRoutes
import pl.everactive.shared.UserDataResponse

@RestController
@HasManagerRole
class ManagerController(
    private val userService: UserService,
) {
    @GetMapping(ApiRoutes.Manager.USER_DATA)
    fun getAllUserData(): UserDataResponse = UserDataResponse(users = userService.getAllUserData())
}
