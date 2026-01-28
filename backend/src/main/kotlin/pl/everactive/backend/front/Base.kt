package pl.everactive.backend.front

import kotlinx.css.LinearDimension
import kotlinx.css.Margin
import kotlinx.css.body
import kotlinx.css.margin
import kotlinx.css.rem
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import pl.everactive.backend.utils.css

private val baseCss = css {
    body {
        margin = Margin(2.rem)
    }
}

abstract class BasePage(private val titleText: String) {
    abstract fun BODY.body()

    protected open fun HEAD.head() {
        title { +titleText }

        meta {
            charset = "utf-8"
        }

        meta {
            name = "viewport"
            content = "width=device-width, initial-scale=1"
        }

        script(type = "text/javascript", src = "/webjars/htmx.org/dist/htmx.min.js") {}
        script(type = "text/javascript", src = "/webjars/hyperscript.org/dist/_hyperscript.min.js") {}

        link(rel = "stylesheet", href = "/webjars/pico/css/pico.classless.min.css")

        style(type = "text/css") {
            unsafe { +baseCss }
        }
    }

    fun render(): String = createHTML().html {
        head {
            head()
        }
        body {
            body()
        }
    }
}
