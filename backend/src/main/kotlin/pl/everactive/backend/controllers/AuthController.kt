package pl.everactive.backend.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pl.everactive.backend.services.TokenService
import pl.everactive.backend.services.UserService
import pl.everactive.backend.services.UserService.RegistrationResult
import pl.everactive.shared.ApiResult
import pl.everactive.shared.ApiResult.Error
import pl.everactive.shared.ApiResult.Success
import pl.everactive.shared.ApiRoutes
import pl.everactive.shared.dtos.LoginRequest
import pl.everactive.shared.dtos.LoginResponse
import pl.everactive.shared.dtos.RegisterRequest

@RestController
class AuthController(
    private val tokenService: TokenService,
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
) {
    @PostMapping(ApiRoutes.Auth.LOGIN)
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val token = tokenService.generate(authentication)

        return LoginResponse(token)
    }

    @PostMapping(ApiRoutes.Auth.REGISTER)
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<ApiResult<LoginResponse>> = when (val result = userService.register(request)) {
        RegistrationResult.Success -> {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )

            val token = tokenService.generate(authentication)

            ResponseEntity.ok(Success(LoginResponse(token)))
        }

        RegistrationResult.AlreadyExists -> {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Error(Error.Type.Conflict))
        }

        is RegistrationResult.Failure -> {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Error(Error.Type.Validation, result.reason))
        }
    }
}
