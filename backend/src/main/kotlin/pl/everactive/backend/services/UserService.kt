package pl.everactive.backend.services

import io.konform.validation.Invalid
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pl.everactive.backend.config.Role
import pl.everactive.backend.entities.UserEntity
import pl.everactive.backend.repositories.RuleEventRepository
import pl.everactive.backend.repositories.UserRepository
import pl.everactive.shared.UserDataDto
import pl.everactive.shared.dtos.RegisterRequest
import java.time.ZoneOffset

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userStateService: UserStateService,
    private val ruleEventRepository: RuleEventRepository,
) : UserDetailsService {
    // FIXME: this is terrible
    fun getAllUserData(): List<UserDataDto> {
        val ruleEventsByUserId = ruleEventRepository.getLatestForAllUsersAndAllRules()
            .groupBy { checkNotNull(it.user.id) }
            .mapValues { entry ->
                entry.value.map {
                    UserDataDto.RuleStatus(
                        rule = it.rule.rule,
                        timestamp = it.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                        isViolated = it.isFailed,
                    )
                }
            }

        return userRepository.findAll().map {
            UserDataDto(
                email = it.email,
                name = it.name,
                role = it.role.toString(),
                group = it.group?.let { group -> UserDataDto.Group(name = group.name) },
                state = userStateService.getStateSnapshot(it).run {
                    UserDataDto.State(
                        lastLocation = lastLocation?.run { UserDataDto.State.Location(latitude, longitude) },
                        lastMoveTime = lastMoveTime?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
                        lastEventTime = lastEventTime?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
                        isSos = isSos,
                        fellRecently = fellRecently,
                    )
                },
                ruleStatus = ruleEventsByUserId[checkNotNull(it.id)] ?: emptyList(),
            )
        }
    }

    override fun loadUserByUsername(username: String): UserDetails =
        userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException.fromUsername(username)

    fun register(request: RegisterRequest): RegistrationResult {
        val validationResult = RegisterRequest.validate(request)
        if (validationResult is Invalid) {
            return RegistrationResult.Failure(validationResult.errors.first().message)
        }

        if (userRepository.existsByEmail(request.email)) {
            return RegistrationResult.AlreadyExists
        }

        val user = UserEntity(
            email = request.email,
            name = request.name,
            password = passwordEncoder.encode(request.password)!!,
            role = Role.User
        )

        userRepository.save(user)

        return RegistrationResult.Success
    }

    sealed interface RegistrationResult {
        data object Success : RegistrationResult
        data object AlreadyExists : RegistrationResult
        data class Failure(val reason: String) : RegistrationResult
    }
}
