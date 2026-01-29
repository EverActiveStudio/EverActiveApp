package pl.everactive.backend.front

import kotlinx.html.*

class IndexPage(
    private val isError: Boolean,
    private val isRegistered: Boolean = false,
) : BasePage("Everactive") {
    override fun BODY.body() {
        div(classes = "centered-container") { // Ta klasa wyśrodkuje tylko ten formularz
            style = "min-height: 90vh;"
            article { // Karta Pico CSS
                header {
                    h3 { style = "margin: 0; text-align: center;"; +"Logowanie" }
                }

                form(action = "/login", method = FormMethod.post) {
                    label {
                        +"Email"
                        input(type = InputType.text, name = "username") {
                            placeholder = "Twój email..."
                            required = true
                        }
                    }
                    label {
                        +"Hasło"
                        input(type = InputType.password, name = "password") {
                            placeholder = "Hasło"
                            required = true
                        }
                    }
                    input(type = InputType.submit) {
                        value = "Zaloguj się"
                        style = "width: 100%;"
                    }

                    if (isError) {
                        p { style = "color: #d81b60; font-size: 0.8rem; text-align: center;"; +"Błędny email lub hasło." }
                    }
                }
            }
        }
    }
}
