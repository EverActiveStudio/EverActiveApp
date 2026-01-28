package pl.everactive.backend.controllers

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.front.ManagerDashboardPage
import pl.everactive.backend.security.HasManagerRole
import pl.everactive.backend.services.RequestService

@RestController
@RequestMapping("/manager")
@HasManagerRole
class ManagerController(
    private val requestService: RequestService,
) {
    @GetMapping("/")
    fun getManagerDashboard(): String = ManagerDashboardPage(
        managerName = requestService.user.name
    ).render()
}
