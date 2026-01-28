package pl.everactive.backend.front

import kotlinx.html.*

class IndexPage(
    private val isError: Boolean,
) : BasePage("Everactive") {
    override fun BODY.body() {
        h3 {
            +"Log in"
        }

        form(action = "/login", method = FormMethod.post) {
            input(type = InputType.text, name = "username") {
                placeholder = "Email"
            }
            input(type = InputType.password, name = "password") {
                placeholder = "Password"
            }
            input(type = InputType.submit) {
                value = "Log in"
            }

            if (isError) {
                p {
                    +"Invalid username or password."
                }
            }
        }
    }
}
