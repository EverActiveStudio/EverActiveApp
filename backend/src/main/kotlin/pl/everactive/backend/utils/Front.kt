package pl.everactive.backend.utils

import com.inet.lib.less.Less
import kotlinx.css.CssBuilder

inline fun css(block: CssBuilder.() -> Unit): String = Less.compile(
    null,
    CssBuilder()
        .apply(block)
        .toString(),
    true,
)
