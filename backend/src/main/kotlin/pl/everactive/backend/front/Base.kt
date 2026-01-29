package pl.everactive.backend.front

import kotlinx.css.LinearDimension
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import pl.everactive.backend.utils.css

private const val FORM_BG_COLOR = "#0f2027"
private const val TEXT_COLOR = "#ffffff"
private val baseCss = css {
    body {
        margin = Margin(0.rem)
        padding = Padding(2.rem) // Przywracamy padding dla czytelności tabel
        backgroundColor = Color(FORM_BG_COLOR)
        color = Color(TEXT_COLOR)
        minHeight = 100.vh
        fontFamily = "sans-serif"
    }

    // Klasa pomocnicza do centrowania formularzy logowania/rejestracji
    ".centered-container" {
        display = Display.flex
        flexDirection = FlexDirection.column
        justifyContent = JustifyContent.center
        alignItems = Align.center
        padding = Padding(0.rem)
    }

    "article" {
        width = 100.pct
        maxWidth = 550.px
        backgroundColor = Color(FORM_BG_COLOR)
        borderRadius = 8.px
        border = Border(1.px, BorderStyle.solid, Color("#2c5364"))
        padding = Padding(2.rem)
    }

    "table" {
        width = 100.pct
        marginTop = 2.rem
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
