package pl.everactive.backend.front

import kotlinx.html.*
import kotlinx.html.BODY
import pl.everactive.backend.config.Role
import pl.everactive.backend.entities.UserEntity

// --- Nowa Paleta: Ciemny Niebieski ---
private const val FORM_BG_COLOR = "#0f2027"       // Ciemny niebieski (prawie granat) jako tło formularza
private const val TEXT_COLOR = "#ffffff"          // Biały tekst dla kontrastu
private const val INPUT_BG_COLOR = "#203a43"      // Nieco jaśniejszy niebieski dla pól
private const val BORDER_COLOR = "#2c5364"        // Obramowania
private const val ACCENT_COLOR = "#4ca1af"        // Turkusowy/Jasnoniebieski akcent przycisku

class UserEditPage(
    private val user: UserEntity,
    private val error: String? = null
) : BasePage("Edycja użytkownika") {

    // Styl pól formularza (prosty, spójny z niebieskim tłem)
    private val commonInputStyle = """
        width: 100%;
        padding: 10px;
        margin-top: 5px;
        margin-bottom: 15px;
        background-color: $INPUT_BG_COLOR;
        color: $TEXT_COLOR;
        border: 1px solid $BORDER_COLOR;
        border-radius: 4px;
        box-sizing: border-box;
    """.trimIndent()

    override fun BODY.body() {
        // Główny kontener (już bez czarnego tła na całą stronę)
        div {
            // Centrowanie i marginesy
            style = "max-width: 500px; margin: 3rem auto; font-family: sans-serif;"

            // --- Karta Formularza (Ciemnoniebieska) ---
            div {
                style = """
                    padding: 2rem;
                    background-color: $FORM_BG_COLOR;
                    color: $TEXT_COLOR;
                    border-radius: 8px;
                    box-shadow: 0 4px 15px rgba(0,0,0,0.2);
                """.trimIndent()

                h2 {
                    style = "text-align: center; margin-top: 0;"
                    +"Edycja Użytkownika"
                }

                // Obsługa błędu
                if (error != null) {
                    div {
                        style = "background-color: #e74c3c; color: white; padding: 10px; border-radius: 4px; margin-bottom: 15px; text-align: center;"
                        +"Błąd: $error"
                    }
                }

                form(action = "/manager/users/${user.id}/save", method = FormMethod.post) {

                    // Pole Imię
                    label {
                        +"Imię i Nazwisko"
                        input(type = InputType.text) {
                            name = "name"
                            value = user.name
                            required = true
                            style = commonInputStyle
                        }
                    }

                    // Pole Email
                    label {
                        +"Email (Login)"
                        input(type = InputType.email) {
                            value = user.email
                            readonly = true
                            // Lekko przyciemniony dla readonly
                            style = commonInputStyle + "opacity: 0.7; cursor: not-allowed;"
                        }
                    }

                    // Pole Rola
                    label {
                        +"Rola"
                        select {
                            name = "role"
                            style = commonInputStyle
                            Role.entries.forEach { roleEnum ->
                                option {
                                    value = roleEnum.name
                                    selected = (user.role == roleEnum)
                                    +roleEnum.name
                                }
                            }
                        }
                    }

                    // Przyciski
                    div {
                        style = "margin-top: 1rem; display: flex; gap: 10px;"

                        button(type = ButtonType.submit) {
                            style = """
                                flex: 1;
                                padding: 10px;
                                background-color: $ACCENT_COLOR;
                                color: white;
                                border: 1px solid $BORDER_COLOR;
                                border-radius: 4px;
                                font-weight: bold;
                                cursor: pointer;
                            """.trimIndent()
                            +"Zapisz"
                        }

                        a(href = "/manager/users") {
                            role = "button"
                            style = """
                                padding: 10px 20px;
                                background-color: transparent;
                                color: $TEXT_COLOR;
                                border: 1px solid $BORDER_COLOR;
                                border-radius: 4px;
                                text-decoration: none;
                                text-align: center;
                            """.trimIndent()
                            +"Anuluj"
                        }
                    }
                }
            }
        }
    }
}
