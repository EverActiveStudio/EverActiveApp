package pl.everactive.shared

object ApiRoutes {
    object Auth {
        const val PREFIX = "/api/auth"

        const val LOGIN = "$PREFIX/login"
        const val REGISTER = "$PREFIX/register"
    }

    object User {
        const val PREFIX = "/api/user"

        const val EVENTS = "$PREFIX/events"
    }

    object Manager {
        const val PREFIX = "/api/manager"

        const val USER_DATA = "$PREFIX/user-data"
    }
}
