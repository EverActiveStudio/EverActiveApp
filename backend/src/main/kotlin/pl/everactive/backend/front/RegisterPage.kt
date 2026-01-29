package pl.everactive.backend.front

import kotlinx.html.*

class RegisterPage(
    private val error: String? = null
) : BasePage("Rejestracja") {
    override fun BODY.body() {
        div(classes = "centered-container") { // Ta klasa wyśrodkuje tylko ten formularz
            style = "min-height: 90vh;"
            article {
                header {
                    h3 { style = "margin: 0; text-align: center;"; +"Nowe konto Managera" }
                }

                form(action = "/register", method = FormMethod.post) {
                    label {
                        +"Imię i Nazwisko"
                        input(type = InputType.text, name = "name") { required = true }
                    }
                    label {
                        +"Email"
                        input(type = InputType.email, name = "email") { required = true }
                    }
                    label {
                        +"Hasło"
                        input(type = InputType.password, name = "password") { required = true }
                    }

                    button(type = ButtonType.submit) {
                        style = "width: 100%; background-color: #4ca1af;"
                        +"Zarejestruj się jako Manager"
                    }
                }

                footer {
                    div {
                        style = "text-align: center;"
                        a(href = "/") { +"Powrót do logowania" }
                    }
                }
            }
        }

    }
}
