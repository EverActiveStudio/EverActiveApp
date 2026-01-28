package pl.everactive.backend.front

import kotlinx.html.BODY
import kotlinx.html.h1

class IndexPage : BasePage("Everactive") {
    override fun BODY.body() {
        h1 {
            +"Hello world"
        }
    }
}
