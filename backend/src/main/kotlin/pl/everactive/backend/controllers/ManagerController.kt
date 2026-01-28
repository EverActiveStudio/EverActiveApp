package pl.everactive.backend.controllers

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import pl.everactive.backend.config.Role
import pl.everactive.backend.entities.UserEntity
import pl.everactive.backend.front.UserCreatePage
import pl.everactive.backend.front.UserEditPage
import pl.everactive.backend.front.UsersPage
import pl.everactive.backend.repositories.UserRepository
import pl.everactive.backend.security.HasManagerRole
import pl.everactive.backend.services.RequestService
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/manager")
@HasManagerRole
class ManagerController(
    private val requestService: RequestService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    // Przekierowanie z dashboardu na listę
    @GetMapping("/")
    fun getManagerDashboard(): RedirectView {
        return RedirectView("/manager/users")
    }

    // Lista użytkowników
    @GetMapping("/users")
    fun listUsers(): String {
        val users = userRepository.findAll().sortedBy { it.id }
        return UsersPage(users).render()
    }

    // --- TWORZENIE UŻYTKOWNIKA ---

    @GetMapping("/users/create")
    fun createUserPage(): String {
        return UserCreatePage().render()
    }

    @PostMapping("/users/{id}/delete")
    fun deleteUser(@PathVariable id: Long): RedirectView {
        try {
            // Sprawdź, czy użytkownik istnieje przed usunięciem (opcjonalne, ale dobre dla logów)
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id)
            }
        } catch (e: Exception) {
            // Tutaj można obsłużyć błąd, np. jeśli użytkownik ma powiązane zdarzenia w bazie
            // Na razie po prostu wracamy do listy
            println("Błąd podczas usuwania użytkownika: ${e.message}")
        }

        return RedirectView("/manager/users")
    }

    @PostMapping("/users/create/save")
    fun saveNewUser(
        @RequestParam(required = false) name: String,
        @RequestParam(required = false) email: String,
        @RequestParam(required = false) role: String,
        @RequestParam(required = false) password: String
    ): Any {
        // Ręczna walidacja - bezpieczniejsza niż automatyczna w tym przypadku
        if (name.isNullOrBlank() || email.isNullOrBlank() || role.isNullOrBlank() || password.isNullOrBlank()) {
            return UserCreatePage(error = "Wszystkie pola są wymagane.").render()
        }

        // Sprawdzenie unikalności emaila
        if (userRepository.findByEmail(email!!) != null) {
            return UserCreatePage(error = "Użytkownik o emailu $email już istnieje.").render()
        }

        try {
            val newUser = UserEntity(
                name = name!!,       // Tutaj mamy pewność, że nie jest null (sprawdziliśmy wyżej)
                email = email,
                password = passwordEncoder.encode(password)!!, // Kodowanie hasła
                role = Role.valueOf(role!!)
            )
            userRepository.save(newUser)
        } catch (e: Exception) {
            return UserCreatePage(error = "Błąd zapisu: ${e.message}").render()
        }

        return RedirectView("/manager/users")
    }

    // --- EDYCJA UŻYTKOWNIKA ---

    @GetMapping("/users/{id}/edit")
    fun editUser(@PathVariable id: Long): String {
        val user = userRepository.findById(id).getOrNull()
            ?: return "Użytkownik nie znaleziony"

        return UserEditPage(user).render()
    }

    @PostMapping("/users/{id}/save")
    fun saveUser(
        @PathVariable id: Long,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) role: String?
    ): RedirectView {
        val user = userRepository.findById(id).getOrNull()

        if (user != null) {
            if (!name.isNullOrBlank()) {
                user.name = name
            }
            if (!role.isNullOrBlank()) {
                try {
                    user.role = Role.valueOf(role)
                } catch (e: Exception) {
                    // Ignorujemy niepoprawną rolę
                }
            }
            userRepository.save(user)
        }

        return RedirectView("/manager/users")
    }
}
