package pl.everactive.backend.front

import kotlinx.html.*
import kotlinx.html.BODY
import pl.everactive.backend.config.Role

// --- Paleta (taka sama jak w edycji) ---
private const val FORM_BG_COLOR = "#0f2027"
private const val TEXT_COLOR = "#ffffff"
private const val INPUT_BG_COLOR = "#203a43"
private const val BORDER_COLOR = "#2c5364"
private const val ACCENT_COLOR = "#4ca1af"

class UserCreatePage(
    private val error: String? = null
) : BasePage("Dodaj użytkownika") {

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
        div {
            style = "max-width: 500px; margin: 3rem auto; font-family: sans-serif;"

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
                    +"Dodaj Nowego Użytkownika"
                }

                if (error != null) {
                    div {
                        style = "background-color: #e74c3c; color: white; padding: 10px; border-radius: 4px; margin-bottom: 15px; text-align: center;"
                        +"Błąd: $error"
                    }
                }

                // Formularz wysyła dane na nowy endpoint
                form(action = "/manager/users/create/save", method = FormMethod.post) {

                    // Pole Imię
                    label {
                        +"Imię i Nazwisko"
                        input(type = InputType.text) {
                            name = "name"
                            required = true
                            style = commonInputStyle
                        }
                    }

                    // Pole Email
                    label {
                        +"Email (Login)"
                        input(type = InputType.email) {
                            name = "email" // Tutaj name jest wymagane
                            required = true
                            style = commonInputStyle
                        }
                    }

                    // Pole Hasło (Nowe pole!)
                    label {
                        +"Hasło"
                        input(type = InputType.password) {
                            name = "password"
                            required = true
                            style = commonInputStyle
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
                                border: 1px solid $ACCENT_COLOR;
                                border-radius: 4px;
                                font-weight: bold;
                                cursor: pointer;
                                font-size: 1rem;
                                line-height: normal;
                            """.trimIndent()
                            +"Utwórz"
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
                                font-size: 1rem;
                                line-height: normal;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                            """.trimIndent()
                            +"Anuluj"
                        }
                    }
                }
            }
        }
    }
}
