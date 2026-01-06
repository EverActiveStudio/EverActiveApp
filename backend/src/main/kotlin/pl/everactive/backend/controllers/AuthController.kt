package pl.everactive.backend.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.services.TokenService
import pl.everactive.backend.services.UserService
import pl.everactive.backend.services.UserService.RegistrationResult
import pl.everactive.shared.ErrorDto
import pl.everactive.shared.dtos.LoginRequest
import pl.everactive.shared.dtos.LoginResponse
import pl.everactive.shared.dtos.RegisterRequest

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val tokenService: TokenService,
    private val userService: UserService,
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

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest) = when (val result = userService.register(request)) {
        RegistrationResult.Success -> {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )

            val token = tokenService.generate(authentication)

            ResponseEntity.ok(LoginResponse(token))
        }

        RegistrationResult.AlreadyExists -> {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .build()
        }

        is RegistrationResult.Failure -> {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto(result.reason))
        }
    }
}
