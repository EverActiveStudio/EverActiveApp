package pl.everactive.backend.controllers

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.everactive.shared.dtos.LoginRequest
import pl.everactive.shared.dtos.LoginResponse
import pl.everactive.backend.services.TokenService

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val tokenService: TokenService,
    private val authenticationManager: AuthenticationManager,
) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val token = tokenService.generate(authentication)

        return LoginResponse(token)
    }
}
